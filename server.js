const net = require("net");
const fs = require("fs");

// success
// const url = "www.example.com";
// const url = "monta.if.its.ac.id"; // index.php/berita/lihatBerita
// const url = "info.cern.ch";
// const url = "web.simmons.edu";

// redirection
const url = "sublimebeautifulmajesticverse.neverssl.com";

// failed
// const url = "intip.in";
// const url = "yahoo.com";
// const url = "facebook.com";
// const url = "wikipedia.com";
// const url = "its.ac.id";
// const url = "iris.its.ac.id";
// const url = "ichiro.its.ac.id";
// const url = "linkedin.com";

const port = 80;
// const port = 443;

// ====================== Define variables ======================
let texts = [];
let anchors = [];
let header = "";
let body = "";
let prefix = "";
let chunks = "";

// Create a TCP socket
const createSocket = async (theUrl) => {
  const client = await net.createConnection({ port: port, host: url }, () => {
    console.log("Connected to server!");

    header = "";
    body = "";
    chunks = "";
    prefix = "";
    texts = [];
    anchors = [];

    let path = "/";
    if (theUrl.includes("/")) {
      const prefixEndIndex = theUrl.indexOf("/");
      prefix = theUrl.slice(prefixEndIndex);
      path = theUrl.slice(prefixEndIndex, theUrl.length);
      path += "/";

      theUrl = theUrl.slice(0, prefixEndIndex);
    }

    // Extract the prefix
    const prefixEndIndex = theUrl.indexOf(".");
    prefix = theUrl.slice(0, prefixEndIndex);

    client.write(`GET ${path} HTTP/1.1\r\nHost: ${theUrl}\r\n\r\n`);
    // client.write(`GET /index.php/berita/lihatBerita HTTP/1.1\r\nHost: ${url}\r\n\r\n`);
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

    if (header.length) {
      const headers = header.split("\r\n");
      // console.log("========== Headers:\n", headers);
      // first rule
      const fRule = "HTTP/"; // +4 chars

      const startingCode = headers[0].indexOf(fRule) + (fRule.length + 4);
      const lastCode = startingCode + 3;
      const Code = headers[0].slice(startingCode, lastCode);

      const message = headers[0].slice(lastCode + 1, headers[0].length);
      if (!(Code >= 200 && Code < 300)) {
        console.log("========== ERROR ===========");
        console.log("========== Status Code : ", Code);
        console.log("========== Error Message: ", message);
        console.log("============================");
      }
    }

    console.log("========== HEADER\n", header);
    console.log("========== BODY:\n", body);
    setTag(body);
    console.log("text: ", texts);
    console.log("link: ", anchors);
    console.log("Connection closed!");

    // redirection checking
    if (body.includes("window.location.href")) {
      const theString = "window.location.href";
      let firstInd = body.indexOf("window.location.href");
      let redirectedUrl = "";
      for (let i = firstInd + theString.length; i < body.length; i++) {
        if (body[i] === ";") {
          break;
        }
        redirectedUrl += body[i];
      }
      redirectedUrl = redirectedUrl.slice(redirectedUrl.indexOf("=") + 1);
      while (redirectedUrl.indexOf("'") > -1 || redirectedUrl.indexOf('"') > -1) {
        redirectedUrl = redirectedUrl.replace("'", "");
        redirectedUrl = redirectedUrl.replace('"', "");
      }
      while (redirectedUrl.indexOf(" ") > -1) {
        redirectedUrl = redirectedUrl.replace(" ", "");
      }
      if (isValidUrl(redirectedUrl)) {
        const prefixEndIndex = redirectedUrl.indexOf(".");
        redirectedUrl = redirectedUrl.slice(prefixEndIndex, redirectedUrl.length);

        redirectedUrl = `${prefix}${redirectedUrl}`;
        console.log("========== redirected ==========");
        console.log("valid URL : ", redirectedUrl);
        createSocket(redirectedUrl);
      }
    }
  });
};

// ====================== Define functions ======================
const validUrlRegex = /^(ftp|http|https):\/\/[^ "]+$/;

function isValidUrl(url) {
  return validUrlRegex.test(url);
}

const findLink = (text) => {
  return text.match(/<a\s[^>]*href=(["'])(.*?)\1[^>]*>/i)[2];
};

const setTag = (text) => {
  const lenText = text.length;

  let firstIndex = 0;
  let lastIndex = 0;
  let isText = false;
  let firstIndAnchor = 0;
  let lastIndAnchor = 0;
  let isAnchor = false;
  for (let i = 0; i < lenText; i++) {
    if (text[i] === "<" && i + 1 < lenText && text[i + 1] === "/") {
      lastIndex = i;
      const tempText = text.slice(firstIndex, lastIndex);
      if (
        tempText.length !== 0 &&
        !tempText.includes("function()") &&
        !tempText !== " " &&
        !tempText.includes("<style>") &&
        !(tempText.includes("{") && tempText.includes("}")) &&
        isText
      ) {
        texts.push(text.slice(firstIndex, lastIndex));
      }
    } else if (i + 1 < lenText && text[i] === ">") {
      const isforbiddenTag =
        (lenText - 6 > 0 && text.slice(i - 6, i).includes("style")) ||
        text.slice(i - 6, i).includes("script")
          ? true
          : false;

      if (!isforbiddenTag) {
        firstIndex = i + 1;
        isText = true;
      }
    }

    if (text[i] === "<" && i + 1 < lenText && text[i + 1].toLowerCase() === "a") {
      firstIndAnchor = i;
      isAnchor = true;
    } else if (isAnchor && text[i] === ">") {
      lastIndAnchor = i;
      const tempAnc = text.slice(firstIndAnchor, lastIndAnchor + 1);
      if (tempAnc.includes('href="')) {
        anchors.push(findLink(tempAnc));
      }
      firstIndAnchor = 0;
      lastIndAnchor = 0;
      isAnchor = false;
    }
  }
};

// called function to create socket
createSocket(url);
