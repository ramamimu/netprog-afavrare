const net = require("net");
// const url = "testphp.vulnweb.com";
// const url = "http://httpbin.org";
const url = "httpbin.org";

const port = 80;

// ====================== Define variables ======================
let header = "";
let body = "";
let chunks = "";

// ====================== Get uname and pass from input keyboard ======================

// Create a TCP socket
const createSocket = async (theUrl) => {
  const client = await net.createConnection({ port: port, host: url }, () => {
    console.log("Enter username and password [host url username password] :  ");
    process.stdin.on("data", (data) => {
      const input = data.toString().trim().split(" ");
      const host = input[0];
      const url = input[1];
      const uname = input[2];
      const pass = input[3];

      const unamePass = `${uname}:${pass}`;
      let auth = Buffer.from(unamePass).toString("base64");

      // httpbin.org/basic-auth/foo/bar
      // clue: httpbin.org basic-auth foo bar
      // client.write(
      //   `GET /basic-auth/foo/bar HTTP/1.1\r\nHost: httpbin.org\r\nAuthorization: Basic Zm9vOmJhcg==\r\n\r\n`
      // );

      const request = `GET /${url}/${uname}/${pass} HTTP/1.1\r\nHost: ${host}\r\nAuthorization: Basic ${auth}\r\n\r\n`;
      console.log(request);

      client.write(request);
    });
  });

  client.on("data", async (data) => {
    const response = data.toString();
    chunks += response;
    await client.end();
  });

  await client.on("error", (err) => {
    console.error(err);
  });

  await client.on("end", () => {
    const separatorIndex = chunks.indexOf("\r\n\r\n");

    if (separatorIndex >= 0) {
      header = chunks.slice(0, separatorIndex);
      body = chunks.slice(separatorIndex + 4);
    }

    console.log("========== HEADER\n", header);
    console.log("========== BODY:\n", body);
  });
};

// called function to create socket
createSocket(url);
