import getPixels from "get-pixels";

export default (function() { 
    let service = {
        getHistogram: getHistogram,        
    };

    // vypocte z obrazku histogram 64 barev
	function getHistogram(imgname){        
        return new Promise((resolve) => {
            getPixels(`./images/${imgname}.jpg`, function (err, img) {
                let histogram = [];
                let image = img.data;
                let color = [];
                let r,g,b;
                let width = img.shape[0];
                let height = img.shape[1];
                let a = 1.0 / (width * height);

                for (var i = 0, n = image.length; i < n; i += 4) {
                    let apha = image[i + 3];
                    let red = image[i + 0];
                    let green = image[i + 1];
                    let blue = image[i + 2];
                    
                    r = parseInt(Math.floor(red / 63.75));
                    g = parseInt(Math.floor(green / 63.75));
                    b = parseInt(Math.floor(blue / 63.75));
                    
                    if (r > 3) { 
                        r = 3;
                    }
                    if(g > 3) {
                        g = 3;
                    }
                    if(b > 3) {
                        b = 3;
                    }

                    let index = r*16 + g*4 + b;
                    if (!histogram[index]) {
                        histogram[index] = 0;
                    }
                    histogram[index] += a;
                }
                resolve(histogram);
            });
        });
	}

    return service;
})();