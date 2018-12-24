# Play Ebean HTTP Query


[![Latest release](https://img.shields.io/badge/latest_release-18.12u3-orange.svg)](https://github.com/0xbaadf00d/play-ebean-httpquery/releases)
[![JitPack](https://jitpack.io/v/0xbaadf00d/play-ebean-httpquery.svg)](https://jitpack.io/#0xbaadf00d/play-ebean-httpquery)
[![Build](https://img.shields.io/travis-ci/0xbaadf00d/play-ebean-httpquery.svg?branch=master&style=flat)](https://travis-ci.org/0xbaadf00d/play-ebean-httpquery)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/0xbaadf00d/play-ebean-httpquery/master/LICENSE)

Ebean filters generator from HTTP query string arguments.
*****

## Add play-ebean-httpquery to your project

#### build.sbt

    resolvers += "jitpack" at "https://jitpack.io"

    libraryDependencies += "com.github.0xbaadf00d" % "play-ebean-httpquery" % "release~YY.MM"


#### application.conf

    # Play Modules
    # ~~~~~
    play.modules.enabled += "com.zero_x_baadf00d.play.module.ebean.EbeanHttpQueryBinder"


    # Ebean Http Query
    # ~~~~~
    # https://github.com/0xbaadf00d/play-ebean-httpquery
    ebeanHttpQuery {
        ignorePatterns = ["pattern_1", "pattern_n"]
    
        fieldAliases {
            "pattern_to_find" = "replace_last_word_with"
            ".*\\.?businessActorRef" = "businessActor"
        }
    }


## Handbook

### Basic operators

|   Operator  |                   Description                       |         Accepted Value Types        |
|-------------|-----------------------------------------------------|-------------------------------------|
| eq          | Is equal to                                         | NUMBER STRING BOOLEAN DATETIME UUID |
| ne          | Is not equal to                                     | NUMBER STRING BOOLEAN DATETIME UUID |
| gt          | Is greated than                                     | NUMBER DATETIME                     |
| gte         | Is greater than or equal to                         | NUMBER DATETIME                     |
| lt          | Is lower than                                       | NUMBER DATETIME                     |
| lte         | Is lower than or equal to                           | NUMBER DATETIME                     |
| like        | Is like to                                          | STRING                              |
| ilike       | Is like to (insensitive)                            | STRING                              |
| contains    | Contains                                            | STRING                              |
| icontains   | Contains (insensitive)                              | STRING                              |
| startswith  | The string must starts with                         | STRING                              |
| istartswith | The string must starts with (insensitive)           | STRING                              |
| endswith    | The string must ends with                           | STRING                              |
| iendswith   | The string must ends with (insensitive)             | STRING                              |
| in          | Is contained in the list                            | NUMBER STRING BOOLEAN DATETIME UUID |
| notin       | Is not contained in the list                        | NUMBER STRING BOOLEAN DATETIME UUID |
| between     | Is between boundaries                               | NUMBER STRING BOOLEAN DATETIME UUID |


### Special operators

|   Operator  |                   Description                       |         Accepted Value Types        |
|-------------|-----------------------------------------------------|-------------------------------------|
| isempty     | The FK is empty (ie: One to Many, Many to Many)     | -                                   |
| isnotempty  | The FK is not empty (ie: One to Many, Many to Many) | -                                   |
| isnull      | Must be NULL                                        | -                                   |
| isnotnull   | Must not be NULL                                    | -                                   |
| orderby     | Ordering switch                                     | ASC | DESC                          |


### Examples

_Retrieves all users who are born after 1986 and named alice._
```
user.name__ilike=alice&user.birthday__gte=1987
```

_Retrieves all users who are not named alice._
```
user.name__not__ilike=alice
```

_Retrieves all users created between 1999 and 2001._
```
user.createdAt__between=1999,2001-12-31T23:59:59
```

### Build query

```java
public class MyController extends Controller {

    @Inject
    private EbeanHttpQueryModule ebeanHttpQueryModule;

    public Result index() {
        try {
            final List<Album> albums = this.ebeanHttpQueryModule
                .buildQuery(Album.class, request())
                .findList();
            return ok(Json.toJson(albums));
        } catch (final PersistenceException ignore) {
            flash("danger", "Bad query");
            final List<Album> albums = Album.find
                .findList();
            return ok(Json.toJson(albums));
        }
    }
}
```

### Register new converter

``` java
public class AccountStatusConverter implements EbeanTypeConverter<AccountStatus> {

    @Override
    public Object convert(final String obj) {
        return AccountStatus.valueOf(obj);
    }

    @Override
    public Class<AccountStatus> getManagedObjectClass() {
        return AccountStatus.class;
    }
}
```

``` java
EbeanTypeConverterManager
    .getInstance()
    .registerConverter(new AccountStatusConverter());
```


## License
This project is released under terms of the [MIT license](https://raw.githubusercontent.com/0xbaadf00d/play-ebean-httpquery/master/LICENSE).
