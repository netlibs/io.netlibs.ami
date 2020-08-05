package io.netlibs.ami.pump.utils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

public class CredentialsProviderSupplier implements AWSCredentialsProvider {

  private AwsCredentialsProvider credentialsProvider;

  public CredentialsProviderSupplier(AwsCredentialsProvider credentialsProvider) {
    this.credentialsProvider = credentialsProvider;
  }

  @Override
  public void refresh() {
    // background handled in aws-sdk 2.0.
  }

  @Override
  public AWSCredentials getCredentials() {

    AwsCredentials creds = credentialsProvider.resolveCredentials();

    if (creds instanceof AwsSessionCredentials) {
      AwsSessionCredentials sessionCreds = (AwsSessionCredentials) creds;
      return new BasicSessionCredentials(sessionCreds.accessKeyId(), sessionCreds.secretAccessKey(), sessionCreds.sessionToken());
    }

    return new BasicAWSCredentials(creds.accessKeyId(), creds.secretAccessKey());

  }

}
