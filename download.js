const https = require("https");
const fs = require("fs");

const fileUrl =
  "https://iris.its.ac.id/files/publication/Ball_Position_Transformation_with_Artificial_Intelligence_Based_on_Tensorflow_Libraries.pdf";
const filePath = "./download/single/downloaded.pdf";

const fileURLs = [
  "https://iris.its.ac.id/files/publication/Ball_Position_Transformation_with_Artificial_Intelligence_Based_on_Tensorflow_Libraries.pdf",
  "https://iris.its.ac.id/files/publication/Ball_Position_Transformation_with_Artificial_Intelligence_Based_on_Tensorflow_Libraries.pdf",
  "https://iris.its.ac.id/files/publication/ADVANCE_ATTACK_AND_DEFENSE_STRATEGY_ALGORITHM_WITH_DYNAMIC_ROLE_ASSIGNMENT_FOR_WHEELED_SOCCER_ROBOT.pdf",
  "https://iris.its.ac.id/files/publication/Sistem_Pengenalan_Suara_untuk_Perintah_pada_Robot_Sepak_Bola_Beroda.pdf",
  "https://iris.its.ac.id/files/publication/Penggunaan_Kamera_Global_untuk_Menentukan_Posisi_Robot_pada_Lapangan.pdf",
];

const file = fs.createWriteStream(filePath);

https
  .get(fileUrl, (response) => {
    response.pipe(file);

    response.on("end", () => {
      console.log("File downloaded successfully!");
    });
  })
  .on("error", (error) => {
    console.error(`Error downloading file: ${error}`);
  });

for (let i = 0; i < fileURLs.length; i++) {
  const file = fs.createWriteStream(`./download/multiple/file-ke-${i + 1}.pdf`);

  https
    .get(fileURLs[i], (response) => {
      response.pipe(file);

      response.on("end", () => {
        console.log("File downloaded successfully!");
      });
    })
    .on("error", (error) => {
      console.error(`Error downloading file: ${error}`);
    });
}
