const html = '<div class="example">Hello, World!</div>';

const regex = /<(\w+)[^>]*>(.*?)<\/\1>/;

const match = regex.exec(html);

if (match) {
  const text = match[2];
  console.log(text);
} else {
  console.log("No match found");
}
