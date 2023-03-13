const https = require("https");

// const url = "https://portal.its.ac.id/";
// const url = "https://www.w3schools.com/";
// const url = "https://www.twilio.com/";
// const url = "https://www.geeksforgeeks.org/";
// const url = "https://iris.its.ac.id/publication/";
const url = "https://www.google.com";
// const url = "https://intip.in/GPOY/";
// const url = "https://intip.in/uRHW/";
let texts = [];
let anchors = [];

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

https
  .get(url, (res) => {
    let data = "";

    res.on("data", (chunk) => {
      data += chunk;
    });

    res.on("end", () => {
      setTag(data);
      console.log("text: ", texts);
      console.log("link: ", anchors);
    });
  })
  .on("error", (err) => {
    console.error(err);
  });
