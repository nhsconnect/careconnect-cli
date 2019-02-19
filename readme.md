#CLI Reference


## ODS Import

java -jar cc-cli.jar "upload-ods" -t http://localhost/careconnect-ri/STU3


## Validate

javac target/cc-cli.jar validate -n /Development/QRISK-ME.json

## Load examples

javac target/cc-cli.jar upload-examples -t http://127.0.0.1:8186/ccri-fhir/STU3 -a



# Docker Notes


docker build . -t thorlogic/ccri-dataload

-- docker tag ccri-document thorlogic/ccri-document

docker push thorlogic/ccri-dataload