function getData(event) {
    event.preventDefault();
    getURL(document.getElementsByName('url').item(0).value,
        document.getElementsByName('qr').item(0).checked);
}

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
            if (qr /*=== true*/) {
                getQR(response.url, response.properties.hash);
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

function getQR(url, hash){
    var widthProp = "-webkit-fill-available"

        fetch(`/${hash}/qr`).then(response => {
                if(!response.ok) {
                    throw Error(response.status)
                }
                return response.blob()
            })
            .then(blob => {
                var image = URL.createObjectURL(blob);
                document.getElementById('result').innerHTML =
                    `<div class='alert alert-success lead'>
                        <a target='_blank' href="${url}">${url}</a>
                        <br>
                        <img src="${image}" style="width: ${widthProp};margin: 1rem 0; border-radius: 5%; border: 15px solid white"/>
                    </div>`;
            })
            .catch(() =>
                document.getElementById('result').innerHTML =
                    `<div class='alert alert-danger lead'>ERROR</div>`
            );
}

document.getElementById('shortener').addEventListener('submit', getData);

/*$(document).ready(
    function () {
        $("#shortener").submit(
            function (event) {
                event.preventDefault();
                $.ajax({
                    type: "POST",
                    url: "/api/link",
                    data: $(this).serialize(),
                    success: function (msg, status, request) {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
                            + "</a></div>");
                    },
                    error: function () {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
    });

*/