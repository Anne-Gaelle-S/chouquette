# gdal2tiles2hdfs

## Usage

### Production

    docker-compose -f docker-compose.yml up

Service avaible at: http://localhost:51264


## Development

    docker-compose up
    docker attach gdal2tiles2hdfs # sbt shell

Service avaible at: http://localhost:51264


## Routes

### `POST /gdal2tiles2hdfs`

*Request:*
```json
{
    "url": string,     // url to download image from
    "hdfsPath": string // path to store to hdfs
}
```

Downloads `url` on in local file system, calls `gdal2tiles.py` on it and puts the result at `hdfsPath` on HDFS. Returns the route where the status can be checked.

*Response:*
```http
Status: 202

{
    "status": string
}
```


### `GET /status/<uuid>`

<!-- Returns the status for the submited `gdal2tiles2hdfs` job `uuid`.

*Response:*
```http
Status: 201 # finished, the files were added to HDFS
Status: 202 # not finished yet
Status: 404 # no such job
``` -->

## Todo

- add options to give to `gdal2tiles.py`
