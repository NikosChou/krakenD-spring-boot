docker run --rm -it -p9000:9000 --add-host spring-boot:192.168.56.1 -v $PWD/krakenD:/etc/krakend/ devopsfaith/krakend:2.0 run --config /etc/krakend/krakend.json 
