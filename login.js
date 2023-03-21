const net = require("net");
const url = "testphp.vulnweb.com";

const port = 80;

// ====================== Define variables ======================
let header = "";
let body = "";
let chunks = "";

// Create a TCP socket
const createSocket = async (theUrl) => {
  const client = await net.createConnection({ port: port, host: url }, () => {
    // access the protected page without cookie
    client.write(`GET /userinfo.php HTTP/1.1\r\nHost: testphp.vulnweb.com\r\n\r\n`);
    // set the cookie
    // client.write(
    //   `POST /userinfo.php HTTP/1.1\r\nHost: testphp.vulnweb.com\r\nCookie: login=test%2Ftest\r\n\r\n`
    // );
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
