const tls = require("tls");
const net = require("net");
const fs = require("fs");

// success
// classroom.its.ac.id
// iris.its.ac.id,

const options = {
  host: "stash.compciv.org",
  port: 443,
  rejectUnauthorized: true, // set to false to accept self-signed certificates
};

const socket = net.createConnection(options, () => {
  const tlsSocket = tls.connect(options, () => {
    console.log("TLS connection established");
    const request = [
      "GET /ssa_baby_names/names.zip HTTP/1.1",
      `Host: ${options.host}`,
      "Connection: close",
      "",
      "",
    ].join("\r\n");
    tlsSocket.write(request);
  });

  const chunks = [];
  let totalSize = 0;
  tlsSocket.on("data", (data) => {
    chunks.push(data);
    totalSize += data.length;
  });

  tlsSocket.on("end", () => {
    console.log(`Received ${totalSize} bytes`);
    const buffer = Buffer.concat(chunks);
    console.log(chunks);
    console.log(buffer.toString());
    // fs.writeFile('file.bin', buffer, (error) => {
    //   if (error) {
    //     console.error(`Error writing file: ${error}`);
    //   } else {
    //     console.log('File saved');
    //   }
    //   tlsSocket.end();
    // });
  });
});

socket.on("error", (error) => {
  console.error(`Error: ${error}`);
});

socket.on("close", () => {
  console.log("Socket closed");
});
