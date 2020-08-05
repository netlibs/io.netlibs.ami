## AMI Event Pump

This process simply connects to a local AMI port and streams the events to AWS Kinesis.  The image will exit if an error occurs, or it can not connect.

## AMI Credentials

Pass the password with `-u <username>`.  You can either pass a password with `-p`, or provide path to `manager.conf` with `-f`, in which case it will read the password from the configuration directly.

## AWS Credentials

the pump uses the AWS Kinesis producer, which supports aggregation and buffering.  

It uses credentials from the standard AWS-SDK form, e.g directly on EC2, using env vars, or ~/.aws/config.

## Running

!!! the process exists on failure of any form. ensure you specify a restart policy.


To run in EC2:

```
docker run -ti --rm --net=host --mount type=bind,source=/etc/asterisk/manager.conf,target=/etc/asterisk/manager.conf,readonly netlibs/ami2kinesis:latest -u manager -f /etc/asterisk/manager.conf
```

with AWS configured through env:

```
docker run -ti --rm -e AWS_REGION=us-west-2 -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY -e AWS_SESSION_TOKEN <image>
```


