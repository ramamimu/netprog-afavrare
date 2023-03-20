const net = require("net");

const client = net.createConnection({
  host: "www.its.ac.id",
  port: 80,
});

client.on("connect", () => {
  client.write(
    `GET HTTP/1.1\r\nHost: www.its.ac.id\r\nConnection: close\r\nCookie: PHPSESSID=f96bhprh22v8kd84k9jaqc0lb3\r\n\r\n`
  );
});

client.on("data", (data) => {
  console.log(data.toString());
});

client.on("error", (err) => {
  console.log("Error:", err);
});

client.on("end", () => {
  console.log("Connection closed");
});
