const net = require("net");
const fs = require("fs");

const host = "monta.if.its.ac.id";
const port = 80;

const client = net.createConnection(port, host, () => {
  console.log(`Connected to ${host}:${port}`);
  const request = `GET /assets/files/berita/f5216c14df58fa477736a712fcfd5d98.pdf HTTP/1.1\r\nHost: ${host}\r\n\r\n`;
  client.write(request);
});

let headers = "";
let pdfData = "";

client.on("data", (data) => {
  const response = data.toString();
  const index = response.indexOf("\r\n\r\n");
  if (headers === "") {
    headers = response.slice(0, index + 4);
    pdfData = response.slice(index + 4);
  } else {
    pdfData += response;
  }
});

client.on("end", () => {
  fs.writeFile("example.pdf", pdfData, (err) => {
    if (err) throw err;
    console.log("PDF file saved!");
  });
});

client.on("error", (err) => {
  console.error(err);
});
