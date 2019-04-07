# Chouquette

## Usage

### Production

    docker-compose -f docker-compose.yml up

Application avaible at: http://localhost:51264


## Development

    docker-compose up
    docker attach chouquette # sbt shell: run, test, ...

Application avaible at: http://localhost:51264



## Services

### 1. Descriptor


---

### 2. Extractor

#### `POST /extract`

*Request:*
```jsonld
{
    string // sentence where we want to extract the semantics words
}
```

*Response:*
```jsonld
{
    Array of string // array contening the semantics words extracted
}
```

---

### 3. Geolocalisator

#### `POST /geolocalise`

*Request:*
```jsonld
{
    Array of string // array contening the semantics words extracted
}
```

*Response:*
```jsonld
{
    "places": [
        {
            "long": double // longitude of the first place
            "lat": double // latitude of the first place
        },
        {
            "long": double // longitude of the second place
            "lat": double // latitude of the second place
        }
    ]
}
```

---

### 4. UTMZonator


---

### 5. S2Recuperator

#### `POST /recupere`

By default, the `startDateVal` is 2016-11-05 and the `completionDateVal`is "2016-11-15". 

*Request:*
```jsonld
{
    Array of string // array contening the UTM zone
}
```

*Response:*
```jsonld
{
    "urls": [string] // array where the images urls are
}
```

#### `POST /recupereDate`

*Request:*
```jsonld
{
    "utm": string, // the utm zone
    "startDateVal": string, // the search on peps start from this date
    "completionDateVal": string // the search on peps stop at this date
}
```

*Response:*
```jsonld
{
    "urls": [string] // array where the images urls are
}
```

---

### 67. gdal2tiles2hdfs

#### `POST /gdal2tiles2hdfs`

*Request:*
```jsonld
{
    "imageUrl": string, // url to download image from
    "hdfsHost": string, // host for HDFS server
    "hdfsUser": string, // user for HDFS server
    "hdfsPass": string, // password for HDFS server
    "hdfsPath": string, // path where tiles should be saved on HDFS server
}
```

Downloads `imageUrl` on local file system, calls `gdal2tiles.py` on it, `scp` image and tiles on `hdfsUser@hdfsHost` (with `hdfsPass`), then puts it at `hdfsPath` on HDFS. Returns the route where the status can be checked.

*Response:*
```http
Status: 202
```
```jsonld
{
    "status": string // route where the status can be checked
}
```


#### `GET /status/<jobId>`

<!-- Returns the status for the submited `gdal2tiles2hdfs` job `jobId`.

*Response:*
```http
Status: 201 # finished, the files were added to HDFS
Status: 202 # not finished yet
Status: 404 # no such job
``` -->

---

#### Todo

- add options to give to `gdal2tiles.py`


---

### 8.


---

### 9.


---

### 10.


---
