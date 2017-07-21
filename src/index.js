import utils from "./utils";

// read images
let promiseList = [];
for(let i = 0; i < 1; i++) {
    utils.getHistogram('night')
        .then(histogram => {
            console.log(histogram);
        });

    
}

