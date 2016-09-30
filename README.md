# Play Ebean HTTP Query


[![Latest release](https://img.shields.io/badge/latest_release-16.10-orange.svg)](https://github.com/0xbaadf00d/play-ebean-httpquery/releases)
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



## Usage


#### Build query

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

#### Register new converter

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
