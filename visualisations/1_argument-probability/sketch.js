/**
 * Working out the argument probability from it's claims.
 */

//the data
var tests = [
    {
        title: "A single claim", 
        id: "1", type: "SUPPORTS", probability: "0.5",
        premises: [
            { id: "n", text: "premis text", probability: "0.5" }
        ]
    },
    {
        title: "Two 0.5 claims", 
        id: "2", type: "SUPPORTS", probability: "0.5",
        premises: [
            { id: "n", text: "premis text", probability: "0.5" },
            { id: "n", text: "premis text", probability: "0.5" }
        ]
    },
    {
        title: "Three 0.5 claims", 
        id: "3", type: "SUPPORTS", probability: "0.5",
        premises: [
            { id: "n", text: "premis text", probability: "0.5" },
            { id: "n", text: "premis text", probability: "0.5" },
            { id: "n", text: "premis text", probability: "0.5" }
        ]
    }
];

//the argument "class"
function Argument(data){
    this.id = data.id;
    this.title = data.title;
    this.type = data.type;
    this.probability = data.probability;
    this.premises = data.premises;
}

//Make all the arguments instances of Argument and save to args
var args = [];
tests.forEach(function(testArg, i){
    args.push(new Argument(testArg))
});

//----- Vue! -----

// Define a new component called todo-item
Vue.component('arg-item', {
    props: ['arg'],
    template: `<div class="arg">
        <div class="arg__title">{{arg.title}}</div>
        
    </div>`
});

//run Vue!
new Vue({
    el: '#app',
    data: {
        args: args
    }
})