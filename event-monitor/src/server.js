import http from "node:http";
import { Kafka, logLevel } from "kafkajs";
import { EventStore } from "./store.js";
const store = new EventStore(process.env.EVENT_FILE || "data/events.ndjson");
let connected = false;
const clients = new Set();
const server = http.createServer((req, res) => {
  if (req.url === "/health") {
    res.writeHead(connected ? 200 : 503, {
      "Content-Type": "application/json",
    });
    return res.end(JSON.stringify({ connected, simulation: true }));
  }
  if (req.url === "/events") {
    res.writeHead(200, { "Content-Type": "application/json" });
    return res.end(JSON.stringify(store.recent()));
  }
  if (req.url === "/stream") {
    res.writeHead(200, {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache",
      Connection: "keep-alive",
    });
    res.write(": connected\n\n");
    clients.add(res);
    req.on("close", () => clients.delete(res));
    return;
  }
  res.writeHead(404);
  res.end();
});
const consumer = new Kafka({
  clientId: "cage-monitor",
  brokers: (process.env.KAFKA_BOOTSTRAP || "localhost:9092").split(","),
  logLevel: logLevel.WARN,
}).consumer({ groupId: "cage-monitor-v1" });
async function connect() {
  for (;;) {
    try {
      await consumer.connect();
      await consumer.subscribe({
        topic: "cage.transactions.v1",
        fromBeginning: true,
      });
      connected = true;
      await consumer.run({
        eachMessage: async ({ message }) => {
          if (store.add(message.value.toString()))
            for (const c of clients)
              c.write("data: " + JSON.stringify(store.recent()[0]) + "\n\n");
        },
      });
      return;
    } catch (e) {
      connected = false;
      console.error("Kafka connection unavailable; retrying");
      await new Promise((r) => setTimeout(r, 5000));
    }
  }
}
server.listen(Number(process.env.PORT || 3001), "0.0.0.0");
connect();
async function shutdown() {
  await consumer.disconnect();
  server.close();
  process.exit(0);
}
process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);
