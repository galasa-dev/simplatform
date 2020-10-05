
const form = document.getElementById('form');
const account = document.getElementById('accnr');
const amount = document.getElementById('amount');
var regex = /^[0-9]*$/;
form.addEventListener('submit',e=>{
    e.preventDefault();
    checkInputs();
});
function checkInputs(){
    const acValue = account.value.trim();
    const amValue = amount.value.trim();
    console.log("acValue ="+acValue);
    console.log("acValue = "+amValue);
    if(acValue === ''){
        setErrorFor(account,'Account number cannot be blank');
        console.log('Account number cannot be blank');
    }else if(!regex.test(acValue)){
        setErrorFor(account,'Account number cannot contain letters or symbols');
        console.log('Account number cannot contain letters or symbols');
    }else if(acValue.length!==9){
        console.log(acValue.length);
        setErrorFor(account,'Account number has to contain nine numbers');
        console.log('Account number has to contain nine numbers');
    }else{
        console.log('account nr success');
        sendData(account);
    }
    if(amValue === ''){
        setErrorFor(amount,'Amount cannot be blank');
        console.log('Amount cannot be blank');
    }else if(!regex.test(amValue)){
        setErrorFor(amount,'Amount cannot contain letters or symbols');
        console.log('Amount cannot contain letters or symbols');
    }else{
        setSuccessFor(amount);
        console.log('amount success');
    }
}

function setErrorFor(input, message){
    const fromControl = input.parentElement;
    console.log(fromControl);
    const small = fromControl.querySelector('small');
    console.log(small);
    fromControl.className = 'form-control error';
    small.innerText = message;
}
function setSuccessFor(input){
    const fromControl = input.parentElement;
    console.log(fromControl);
    fromControl.className = 'form-control success';
}

async function sendData(input){
	const data = {};
	
	const response = await fetch(`http://localhost:8080/galasa-simplatform-webapp/update`, {
		method: 'POST',
		headers:{
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(data)
    });
    
    if(response!==200){
        setErrorFor(input,"Account numbere invalid");
    }
    setSuccessFor(input);
}