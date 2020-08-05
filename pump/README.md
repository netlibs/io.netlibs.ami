## AMI Event Pump

This process simply connects to a local AMI port and streams the events to AWS Kinesis.


## Running Locally

```
docker run -ti --rm -e AWS_REGION=us-west-2 -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY -e AWS_SESSION_TOKEN <image>
```



