# Generating the Endpoints client library

In a terminal, do:

    # Locate the real path here...
    cd Aggregato/web_service
    mvn appengine:endpoints_get_client_lib
    # The last part depends on the API name
    cd target/endpoints-client-libs/tvseries
    mvn install
    cd target
    # Copy the generated library to the android libs directory
    cp *.jar Aggregato/android_client/libs

This generates the client library from the API description and places it into Androids `/libs`-directory, where Gradle will pick it up.