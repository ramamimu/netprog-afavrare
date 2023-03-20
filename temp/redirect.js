const https = require("https");

const url = "https://intip.in/GPOY/";

const validUrlRegex = /^(ftp|http|https):\/\/[^ "]+$/;

function isValidUrl(url) {
  return validUrlRegex.test(url);
}

getTheURI = async (url) => {
  https.get(url, async (response) => {
    let data = "";

    response.on("data", (chunk) => {
      data += chunk;
    });

    response.on("end", () => {
      console.log(data);

      if (data.includes("window.location.href")) {
        const theString = "window.location.href";
        let firstInd = data.indexOf("window.location.href");

        let theUrl = "";
        for (let i = firstInd + theString.length; i < data.length; i++) {
          if (data[i] === ";") {
            break;
          }
          theUrl += data[i];
        }

        theUrl = theUrl.slice(theUrl.indexOf("=") + 1);
        while (theUrl.indexOf("'") > -1 || theUrl.indexOf('"') > -1) {
          theUrl = theUrl.replace("'", "");
          theUrl = theUrl.replace('"', "");
        }

        while (theUrl.indexOf(" ") > -1) {
          theUrl = theUrl.replace(" ", "");
        }
        if (isValidUrl(theUrl)) {
          console.log("========== redirected ==========");
          console.log("valid URL : ", theUrl);
          getTheURI(theUrl);
        }
      }
    });
  });
};

getTheURI(url);
