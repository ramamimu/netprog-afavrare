const net = require("net");
const fs = require("fs");

const url = "monta.if.its.ac.id"; // index.php/berita/lihatBerita

const port = 80;
// const port = 443;

// ====================== Define variables ======================
let texts = [];
let anchors = [];

const fileUrl =
  "http://monta.if.its.ac.id/assets/files/berita/f5216c14df58fa477736a712fcfd5d98.pdf";
const filePath = "./download/single/downloaded.pdf";

const fileURLs = [
  "http://monta.if.its.ac.id/assets/files/berita/f5216c14df58fa477736a712fcfd5d98.pdf",
  "http://monta.if.its.ac.id/assets/files/berita/44d80899ea5558208e855e1152f63b75.pdf",
  "http://monta.if.its.ac.id/assets/files/berita/0caa1277547f78cb8823c84850c359ce.pdf",
];

// const file = fs.createWriteStream(filePath);

// Create a TCP socket
const client = net.createConnection({ port: port, host: url }, () => {
  console.log("Connected to server!");

  // client.write(`GET / HTTP/1.1\r\nHost: ${url}\r\n\r\n`);
  // client.write(`GET /index.php/berita/lihatBerita HTTP/1.1\r\nHost: ${url}\r\n\r\n`);
  // one file
  client.write(
    `GET /assets/files/berita/f5216c14df58fa477736a712fcfd5d98.pdf HTTP/1.1\r\nHost: ${url}\r\n\r\n`
  );
});

let header = "";
let body = "";

client.on("data", async (data) => {
  const response = data.toString();

  const separatorIndex = response.indexOf("\r\n\r\n");

  if (separatorIndex >= 0) {
    header = response.slice(0, separatorIndex);
    body = response.slice(separatorIndex + 4);
    // console.log("Header: =>\n", header);
    console.log("Body:", body);
  }
  await client.end();
});

client.on("error", (err) => {
  console.error(err);
});

client.on("end", () => {
  console.log("text: ", texts);
  console.log("link: ", anchors);
  console.log("Connection closed!");
  fs.createWriteStream(filePath, body, (err) => {
    if (err) throw err;
    console.log("PDF file saved!");
  });
});

// ====================== Define functions ======================
