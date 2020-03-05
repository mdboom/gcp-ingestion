# Ingestion Sink

A Java application that runs in Kubernetes, reading input from Google Cloud
Pub/Sub and emitting records to batch-oriented outputs like GCS or BigQuery.
Defined in the `ingestion-sink` package
([source](https://github.com/mozilla/gcp-ingestion/tree/master/ingestion-sink/)).

## Supported Input and Outputs

Supported inputs:

 * Google Cloud PubSub

Supported outputs:

 * Google Cloud PubSub
 * Google Cloud Storage
 * Google Cloud BigQuery

## Configuration

All configuration is controlled by environment variables.

## Output Specification

Depending on the environment variables provided, the application will
automatically determine where to deliver messages.

If `OUTPUT_BUCKET` is specified without `BIG_QUERY_OUTPUT_MODE`, then messages
will be delivered to Google Cloud Storage.

If `OUTPUT_TOPIC` is specified without `OUTPUT_BUCKET` or
`BIG_QUERY_OUTPUT_MODE`, then messages will be delivered to Google Cloud
Pub/Sub.

If `OUTPUT_TABLE` is specified without `BIG_QUERY_OUTPUT_MODE` or with
`BIG_QUERY_OUTPUT_MODE=streaming`, then messages will be delivered to BigQuery
via the streaming api.

If `OUTPUT_TABLE` is specified with `BIG_QUERY_OUTPUT_MODE=file_loads`, then
messages will be delivered to Google Cloud Storage based on `OUTPUT_BUCKET` and
for each blob a notification will be delivered to Google Cloud Pub/Sub based on
`OUTPUT_TOPIC`. Separate instances of ingestion-sink must consume notifications
from Google Cloud Pub/Sub and deliver messages to BigQuery via load jobs.

If `OUTPUT_TABLE` is specified with `BIG_QUERY_OUTPUT_MODE=mixed`, then
messages will be delivered to BigQuery via both the streaming api and load
jobs, and `OUTPUT_BUCKET` is required. If `OUTPUT_TOPIC` is specified then it
will be used the same as with `BIG_QUERY_OUTPUT_MODE=file_loads`, otherwise
load jobs will be submitted by each running instance of ingestion-sink.

If none of the above configuration options are provided, then messages must be
notifications from `BIG_QUERY_OUTPUT_MODE=file_loads` or
`BIG_QUERY_OUTPUT_MODE=mixed`, and the blobs they indicate will be submitted to
BigQuery via load jobs.

### BigQuery

`OUTPUT_TABLE` must be a `tableSpec` of form `dataset.tablename`
or the more verbose `projectId.dataset.tablename`. The values can contain
attribute placeholders of form `${attribute_name}`. To set dataset to the
document namespace and table name to the document type, specify:

    OUTPUT_TABLE='${document_namespace}.${document_type}'

All `-` characters in the attributes will be converted to `_` per BigQuery
naming restrictions. Additionally, document namespace and type values will
be processed to ensure they are in snake case format (`untrustedModules`
becomes `untrusted_modules`).

Defaults for the placeholders using `${attribute_name:-default_value}`
are supported, but likely don't make much sense since it's unlikely that
there is a default table whose schema is compatible with all potential
payloads.

### Attribute placeholders

We support routing individual messages to different output locations based on
the `PubsubMessage` attribute map.  Routing is accomplished by adding
placeholders of form `${attribute_name:-default_value}` to the path.

For example, to route based on a `document_type` attribute, your path might look like:

   OUTPUT_BUCKET=gs://mybucket/mydocs/${document_type:-UNSPECIFIED}/myfileprefix

Messages with `document_type` of "main" would be grouped together and end up in
the following directory:

    gs://mybucket/mydocs/main/

Messages with `document_type` set to `null` or missing that attribute completely
would be grouped together and end up in directory:

    gs://mybucket/mydocs/UNSPECIFIED/

Note that placeholders _must_ specify a default value so that a poorly formatted
message doesn't cause a pipeline exception. A placeholder without a default will
result in an `IllegalArgumentException` on pipeline startup.

File-based outputs support the additional _derived_ attributes
`"submission_date"` and `"submission_hour"` which will be parsed from the value
of the `submission_timestamp` attribute if it exists.
These can be useful for making sure your output specification buckets messages
into hourly directories.

The templating and default syntax used here is based on the
[Apache commons-text `StringSubstitutor`](https://commons.apache.org/proper/commons-text/javadocs/api-release/org/apache/commons/text/StringSubstitutor.html),
which in turn bases its syntax on common practice in bash and other Unix/Linux shells.
Beware the need for proper escaping on the command line (use `\$` in place of `$`),
as your shell may try to substitute in values
for your placeholders before they're passed to `Sink`.

[Google's PubsubMessage format](https://cloud.google.com/pubsub/docs/reference/rest/v1/PubsubMessage)
allows arbitrary strings for attribute names and values. We place the following restrictions
on attribute names and default values used in placeholders:

- attribute names may not contain the string `:-`
- attribute names may not contain curly braces (`{` or `}`)
- default values may not contain curly braces (`{` or `}`)

### Encoding

When writing messages to Google Cloud Storage or BigQuery, the message received
from Google Cloud Pub/Sub will be encoded as a JSON object.

When `OUTPUT_FORMAT` is unspecified or `raw` messages will have bytes encoded as
a `"payload"` field with base64 encoding, and each attribute encoded as field.
This is the format used for `payload_bytes_raw.*` tables.

When `OUTPUT_FORMAT` is `decoded` messages will have bytes encoded as with
`OUTPUT_FORMAT=raw`, but attributes will be encoded using the nested metadata
format of decoded pings. This is the format used for `payload_bytes_decoded.*`
tables.

When `OUTPUT_FORMAT` is `payload` messages will have bytes decoded as JSON, and
transformed to coerce types and use snake case for compatibility with BigQuery.
This is the format used for `*_live.*` tables. This requires specifying a local
path to a gzipped tar archive that contains BigQuery table schemas as
`SCHEMAS_LOCATION`. If messages bytes are compressed then
`INPUT_COMPRESSION=gzip` must also be specified to ensure they are decompressed
before they are decoded as JSON.

### File prefix

Individual files are named by replacing `:` with `-` in the default format discussed in
the "File naming" section of Beam's
[`FileIO` Javadoc](https://beam.apache.org/documentation/sdks/javadoc/2.6.0/org/apache/beam/sdk/io/FileIO.html):

    $prefix-$start-$end-$pane-$shard-of-$numShards$suffix$compressionSuffix

In our case, `$prefix` is determined from the last `/`-delimited piece of the `--output`
path. If you specify a path ending in `/`, you'll end up with an empty prefix
and your file names will begin with `-`. This is probably not what you want, 
so it's recommended to end your output path with a non-empty file prefix. We replace `:`
with `-` because [Hadoop can't handle `:` in file names](https://stackoverflow.com/q/48909921).

For example, given:

    --output=/tmp/output/out

An output file might be:

    /tmp/output/out--290308-12-21T20-00-00.000Z--290308-12-21T20-10-00.000Z-00000-of-00001.ndjson

## Executing

### Locally

If you install Java and maven, you can invoke `mvn` in the following commands
instead of using `./bin/mvn`. Be aware that Java 8 is the target JVM and some
reflection warnings may be thrown on newer versions. Though these are generally
harmless, you may need to comment out the
`<compilerArgument>-Werror</compilerArgument>` line in the `pom.xml` in the git
root.

The provided `bin/mvn` script downloads and runs maven via docker so that less
setup is needed on the local machine. For prolonged development performance is
likely to be significantly better, especially in MacOS, if `mvn` is installed and
run natively without docker.

```bash
# consume messages from the test file, decode and re-encode them, and write to a directory
./bin/mvn compile exec:java

# check that the message was delivered
cat tmp/output/*

# write message payload straight to stdout
./bin/mvn compile exec:java -Dexec.args="\
    --inputFileFormat=json \
    --inputType=file \
    --input=tmp/input.json \
    --outputFileFormat=text \
    --outputType=stdout \
    --errorOutputType=stderr \
"

# check the help page to see types of options
./bin/mvn compile exec:java -Dexec.args=--help

# check the SinkOptions help page for options specific to Sink
./bin/mvn compile exec:java -Dexec.args=--help=SinkOptions
```

### On Kubernetes

```bash
# Pick a bucket to store files in
BUCKET="gs://$(gcloud config get-value project)"

# create a test input file
echo '{"payload":"dGVzdA==","attributeMap":{"host":"test"}}' | gsutil cp - $BUCKET/input.json

# Set credentials; beam is not able to use gcloud credentials
export GOOGLE_APPLICATION_CREDENTIALS="path/to/your/creds.json"

# consume messages from the test file, decode and re-encode them, and write to a bucket
./bin/mvn compile exec:java -Dexec.args="\
    --runner=Dataflow \
    --inputFileFormat=json \
    --inputType=file \
    --input=$BUCKET/input.json \
    --outputFileFormat=json \
    --outputType=file \
    --output=$BUCKET/output \
    --errorOutputType=file \
    --errorOutput=$BUCKET/error \
"

# wait for the job to finish
gcloud dataflow jobs list

# check that the message was delivered
gsutil cat $BUCKET/output/*
```

