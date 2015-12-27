set FNAME=%1
set FNAME=%FNAME:~0,-4%
set CFNAME=%2
set CFNAME=%CFNAME:~0,-6%
::产生私钥::
openssl pkcs12 -in %FNAME%.pfx -nocerts -out %FNAME%temp.key & openssl rsa -in %FNAME%temp.key -out %FNAME%.key
::#产生公钥
openssl pkcs12 -in %FNAME%.pfx -nokeys -clcerts -out %FNAME%temp.cer & openssl x509 -in %FNAME%temp.cer -out %FNAME%.cer
::合并成PEM格式证书
copy %FNAME%.cer+%FNAME%.key+%CFNAME%.chain %FNAME%.pem
del %FNAME%temp.key
del %FNAME%.key
del %FNAME%temp.cer
del %FNAME%.cer
::得到的%FNAME%.pem就是我们需要的结果。
