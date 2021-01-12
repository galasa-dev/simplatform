
const form = document.getElementById('form');
const account = document.getElementById('accnr');
const amount = document.getElementById('amount');
document.getElementById("good").style.visibility = "hidden";
var regexNum = /^[0-9]*$/;
var regexDec = /[0-9]+(\.[0-9][0-9]?)?/;
form.addEventListener('submit', checkInputs);

function checkInputs(e) {
    e.preventDefault();
    
    var acsuc = false;
    var amsuc = false;
    const acValue = account.value.trim();
    const amValue = amount.value.trim();
    if (amValue === '') {
        setErrorFor(amount, 'Amount cannot be blank');
    } else if (!regexDec.test(amValue)) {
        setErrorFor(amount, 'Amount cannot contain letters or symbols');
    } else {
        setSuccessFor(amount);
        amsuc = true;
    }
    if (acValue === '') {
        setErrorFor(account, 'Account number cannot be blank');
    } else if (!regexNum.test(acValue)) {
        setErrorFor(account, 'Account number cannot contain letters or symbols');
    } else if (acValue.length !== 9) {
        setErrorFor(account, 'Account number has to contain nine numbers');
    } else {
        setSuccessFor(account);
        acsuc = true;
    }
    if (acsuc && amsuc) {
        sendData(account, amount);
    }else{
        document.getElementById("good").style.visibility = "hidden";
    }
}

function setErrorFor(input, message) {
    const fromControl = input.parentElement;
    const small = fromControl.querySelector('small');
    fromControl.className = 'form-control error';
    small.innerText = message;
}
function setSuccessFor(input) {
    const fromControl = input.parentElement;
    console.log(fromControl);
    fromControl.className = 'form-control success';
}

async function sendData(input, amount) {//call servlet
    const data = {};
    const acc = input.value.trim();
    const am = amount.value.trim();
    const uri = encodeURI(`${window.location.href}?accnr=${acc}&amount=${am}`);
    console.log(uri);
    const response = await fetch(uri, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    });
    const jsonResponse = await response.json();
    console.log(jsonResponse);
    const code = jsonResponse.statusCode;
    console.log("code = " + code);
    if (code === 200) {
        setSuccessFor(input);
        document.getElementById("good").style.visibility = "visible";

    } else {
        setErrorFor(input, "Account number invalid");
        document.getElementById("good").style.visibility = "hidden";
    }
}