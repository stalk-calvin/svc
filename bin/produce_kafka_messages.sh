#!/bin/bash
set -e

# Get the directory where the script is located
SCRIPT_DIR="$(dirname "$0")"

# Kafka broker address (use Kafka container hostname)
KAFKA_BROKER="kafka:9092"
TOPIC="audit-events"

# Path to the JSON file relative to the script's directory
JSON_FILE="$SCRIPT_DIR/initial-audit-logs.json"

# Check if the JSON file exists
if [[ ! -f "$JSON_FILE" ]]; then
  echo "Error: JSON file $JSON_FILE not found!"
  exit 1
fi

# Wait for Kafka to be ready inside the Kafka container
echo "Waiting for Kafka to be ready..."
until docker exec kafka kafka-topics --bootstrap-server "$KAFKA_BROKER" --list; do
  sleep 1
done

# Produce messages from the JSON file
echo "Producing messages to Kafka topic $TOPIC..."
jq -c '.[]' "$JSON_FILE" | while read -r message; do
  echo "$message" | docker exec -i kafka kafka-console-producer --bootstrap-server "$KAFKA_BROKER" --topic "$TOPIC"
  echo "Produced message: $message"
done

echo "All messages produced successfully."
