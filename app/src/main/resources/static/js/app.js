/**
 * Event handler for the form submit event.
 */
function getData(event) {
    // Prevent the form from submitting via the browser.
    event.preventDefault();
    getURL(document.getElementsByName('url').item(0).value,
        document.getElementsByName('qr').item(0).checked);
}

/**
 * Sends a POST request to a specified API endpoint to create a link with optional QR Code generation.
 *
 * @param {string} url - The URL to be associated with the link.
 * @param {boolean} qr - A flag indicating whether to generate a QR Code (true) or not (false).
 **/
function getURL(url, qr){
    let encodedBody = new URLSearchParams();
    encodedBody.append('url', url);
    encodedBody.append('qr', qr ? "true" : "false");

    const options = {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: encodedBody
    };

    fetch('http://localhost:8080/api/link', options)
        .then(response => {
            if(!response.ok) {
                throw Error(response.status)
            }
            return response.json()
        })
        .then(response => {
            if (response.properties.qr /*=== true*/) {
                //getQR(response.url, response.properties.hash);
                document.getElementById('result').innerHTML =
                    `<div class='alert alert-success lead'>
                    <a target='_blank' href="${response.url}">${response.url}</a>
                    <a target='_blank' href="${response.properties.qr}">${response.properties.qr}</a>
                    </div>`;
            }
            else {
                document.getElementById('result').innerHTML =
                    `<div class='alert alert-success lead'>
                    <a target='_blank' href="${response.url}">${response.url}</a>
                    </div>`;
            }
        })
        .catch(() =>
            document.getElementById('result').innerHTML =
                `<div class='alert alert-danger lead'>ERROR</div>`
        );
}

document.getElementById('shortener').addEventListener('submit', getData);

