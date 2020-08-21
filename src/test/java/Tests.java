/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 - 2020 Thibault Meyer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.zero_x_baadf00d.ebean.PlayEbeanHttpQuery;
import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.Query;
import io.ebean.config.ServerConfig;
import io.ebean.enhance.Transformer;
import io.ebean.enhance.ant.OfflineFileTransform;
import models.Album;
import models.Artist;
import models.Cover;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.*;

/**
 * Tests.
 *
 * @author Thibault Meyer
 * @version 18.07.24
 * @since 16.04.22
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Tests {

    /**
     * Handle to the Ebean server instance.
     *
     * @since 16.04.22
     */
    private static EbeanServer ebeanServer;

    /**
     * Handle to the Ebean Http Query builder instance.
     *
     * @since 16.04.28
     */
    private static PlayEbeanHttpQuery playEbeanHttpQuery;

    /**
     * Handle to the Ebean Http Query builder instance.
     *
     * @since 16.09.06
     */
    private static PlayEbeanHttpQuery playEbeanHttpQueryClone;

    /**
     * Initialize database.
     *
     * @since 16.04.22
     */
    @BeforeClass
    public static void init() {
        Tests.playEbeanHttpQuery = new PlayEbeanHttpQuery();
        Tests.playEbeanHttpQuery.addIgnoredPatterns("fields", "page");
        Tests.playEbeanHttpQuery.addAlias(".*\\.?nothing", "name");
        Tests.playEbeanHttpQuery.addAlias(".*\\.?author", "artist");
        Tests.playEbeanHttpQuery.addAlias("Cover>boap", "album");
        Tests.playEbeanHttpQuery.addAlias("Cover>gnarf", "album.artist.name");
        Tests.playEbeanHttpQueryClone = (PlayEbeanHttpQuery) Tests.playEbeanHttpQuery.clone();

        final Transformer transformer = new Transformer(Tests.class.getClassLoader(), "debug=1");
        final OfflineFileTransform offlineFileTransform = new OfflineFileTransform(
            transformer,
            ClassLoader.getSystemClassLoader(),
            "target"
        );
        offlineFileTransform.process("**");

        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName("default");
        serverConfig.loadFromProperties();
        serverConfig.setDefaultServer(true);
        serverConfig.setRegister(true);
        Tests.ebeanServer = EbeanServerFactory.create(serverConfig);

        try {
            final Scanner scanner = new Scanner(Tests.class.getResourceAsStream("/data-test.txt"));
            while (scanner.hasNextLine()) {
                final String[] artist_info = scanner.nextLine().split("\\|");
                Artist artist = Artist.find.query().where().ilike("name", artist_info[0]).findOne();
                if (artist == null) {
                    artist = new Artist();
                    artist.setName(artist_info[0]);
                    artist.setCreatedAt(
                        new DateTime(artist_info[1])
                            .withZoneRetainFields(DateTimeZone.UTC)
                    );
                    artist.save();
                }

                final Cover cover = new Cover();
                cover.setUrl(scanner.nextLine());
                cover.save();

                final Album album = new Album();
                album.setArtist(artist);
                album.setCover(cover);
                album.setName(scanner.nextLine());
                album.setYear(scanner.nextInt());
                album.setLength(scanner.nextInt());
                album.setAvailable(scanner.nextBoolean());
                album.save();
                scanner.nextLine();
                scanner.nextLine();
            }
            scanner.close();
        } catch (NoSuchElementException ignore) {
        }
    }

    /**
     * @since 16.04.22
     */
    @Test
    public void test001() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("albums.year__notin", new String[]{"1999,2000"});
        args.put("albums.name__ilike", new String[]{"desTINY"});
        args.put("albums.name__istartswith", new String[]{"Des"});
        final Query<Artist> query = Tests.playEbeanHttpQuery
            .buildQuery(Artist.class, args, Tests.ebeanServer.createQuery(Artist.class));
        final List<Artist> artists = query.findList();

        Assert.assertEquals(1, artists.size());
        Assert.assertEquals("Stratovarius", artists.get(0).getName());
    }

    /**
     * @since 16.04.22
     */
    @Test
    public void test002() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("available__eq", new String[]{"yes"});
        args.put("name__orderby", new String[]{"asc"});
        args.put("artist.name__ilike", new String[]{"STRATovarius"});
        final Query<Album> query = Tests.playEbeanHttpQuery.buildQuery(Album.class, args);
        final List<Album> albums = query.findList();

        Assert.assertEquals(2, albums.size());
        Assert.assertEquals("Infinite", albums.get(0).getName());
        Assert.assertEquals("Visions", albums.get(1).getName());
    }

    /**
     * @since 16.04.22
     */
    @Test
    public void test003() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("album.artist.name__like", new String[]{"Stratovarius"});
        args.put("album.year__in", new String[]{"1997,1998,1999,2000,2001,2002"});
        args.put("url__orderby", new String[]{"ASC"});
        final Query<Cover> query = Tests.playEbeanHttpQuery
            .buildQuery(Cover.class, args, Tests.ebeanServer.createQuery(Cover.class));
        final List<Cover> covers = query.findList();

        Assert.assertEquals(3, covers.size());
        Assert.assertEquals("https://domain.local/cover/123897948989", covers.get(0).getUrl());
        Assert.assertEquals("https://domain.local/cover/298372983720", covers.get(1).getUrl());
        Assert.assertEquals("https://domain.local/cover/898656564654", covers.get(2).getUrl());
    }

    /**
     * @since 16.09.30
     */
    @Test
    public void test004() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("boap.author.nothing__like", new String[]{"Stratovarius"});
        args.put("album.year__in", new String[]{"1997,1998,1999,2000,2001,2002"});
        args.put("url__orderby", new String[]{"ASC"});
        final Query<Cover> query = Tests.playEbeanHttpQueryClone
            .buildQuery(Cover.class, args, Tests.ebeanServer.createQuery(Cover.class));
        final List<Cover> covers = query.findList();

        Assert.assertEquals(3, covers.size());
        Assert.assertEquals("https://domain.local/cover/123897948989", covers.get(0).getUrl());
        Assert.assertEquals("https://domain.local/cover/298372983720", covers.get(1).getUrl());
        Assert.assertEquals("https://domain.local/cover/898656564654", covers.get(2).getUrl());
    }

    /**
     * @since 16.09.30
     */
    @Test
    public void test005() {
        final Map<String, String[]> args = new LinkedHashMap<>();
        args.put("album.year__orderby", new String[]{"ASC"});
        args.put("album.artist.name__orderby", new String[]{"ASC"});
        final Query<Album> query = Tests.playEbeanHttpQueryClone
            .buildQuery(Album.class, args, Tests.ebeanServer.createQuery(Album.class));
        final List<Album> albums = query.findList();

        Assert.assertEquals(6, albums.size());
        Assert.assertEquals(Integer.valueOf(1997), albums.get(0).getYear());
        Assert.assertEquals(Integer.valueOf(2001), albums.get(2).getYear());
        Assert.assertEquals(Integer.valueOf(2001), albums.get(3).getYear());
        Assert.assertEquals("Sonata Arctica", albums.get(2).getArtist().getName());
        Assert.assertEquals("Stratovarius", albums.get(3).getArtist().getName());
    }

    /**
     * @since 17.04.18
     */
    @Test
    public void test006() {
        final Map<String, String[]> args = new LinkedHashMap<>();
        args.put("albums__isnotempty", new String[]{""});
        final Query<Artist> query = Tests.playEbeanHttpQueryClone
            .buildQuery(Artist.class, args, Tests.ebeanServer.createQuery(Artist.class));
        final List<Artist> artists = query.findList();

        Assert.assertEquals(3, artists.size());
    }

    /**
     * @since 17.04.18
     */
    @Test
    public void test007() {
        final Map<String, String[]> args = new LinkedHashMap<>();
        args.put("albums__isempty", new String[]{""});
        final Query<Artist> query = Tests.playEbeanHttpQueryClone
            .buildQuery(Artist.class, args, Tests.ebeanServer.createQuery(Artist.class));
        final List<Artist> artists = query.findList();

        Assert.assertEquals(0, artists.size());
    }

    /**
     * @since 17.07.07
     */
    @Test
    public void test008() {
        final Map<String, String[]> args = new LinkedHashMap<>();
        args.put("gnarf", new String[]{"Stratovarius"});
        final Query<Cover> query = Tests.playEbeanHttpQueryClone
            .buildQuery(Cover.class, args, Tests.ebeanServer.createQuery(Cover.class));
        final List<Cover> covers = query.findList();

        Assert.assertEquals(3, covers.size());
    }

    /**
     * @since 18.06.18
     */
    @Test
    public void test009() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("author__eq", new String[]{"2"});
        final Query<Album> query = Tests.playEbeanHttpQuery.buildQuery(Album.class, args);
        final List<Album> albums = query.findList();

        Assert.assertEquals(3, albums.size());
    }

    /**
     * @since 18.07.23
     */
    @Test
    public void test010() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("author__ne", new String[]{"2"});
        final Query<Album> query = Tests.playEbeanHttpQuery.buildQuery(Album.class, args);
        final List<Album> albums = query.findList();

        Assert.assertEquals(3, albums.size());
    }

    /**
     * @since 18.07.23
     */
    @Test
    public void test011() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("artist.name__not__ilike", new String[]{"STRATovarius"});
        final Query<Album> query = Tests.playEbeanHttpQuery.buildQuery(Album.class, args);
        final List<Album> albums = query.findList();

        Assert.assertEquals(3, albums.size());
    }

    /**
     * @since 18.07.23
     */
    @Test
    public void test012() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("createdAt__eq", new String[]{"1984-08-21T18:45:05"});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertFalse(artists.isEmpty());
        Assert.assertEquals("Stratovarius", artists.get(0).getName());
    }

    /**
     * @since 18.07.24
     */
    @Test
    public void test013() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("createdAt__not__eq", new String[]{"1984-08-21T18:45:05"});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertFalse(artists.isEmpty());
        Assert.assertEquals("Dreamtale", artists.get(0).getName());
    }

    /**
     * @since 18.07.24
     */
    @Test
    public void test014() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("createdAt__ne", new String[]{"1984-08-21T18:45:05"});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertFalse(artists.isEmpty());
        Assert.assertEquals("Dreamtale", artists.get(0).getName());
    }

    /**
     * @since 18.07.24
     */
    @Test
    public void test015() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("createdAt__eq", new String[]{"1999"});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertFalse(artists.isEmpty());
        Assert.assertEquals("Dreamtale", artists.get(0).getName());
    }

    /**
     * @since 18.12.17
     */
    @Test
    public void test016() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("id__in", new String[]{""});
        args.put("id__notin", new String[]{""});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertTrue(artists.isEmpty());
    }

    /**
     * @since 18.12.17
     */
    @Test
    public void test017() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("name__iendswith", new String[]{""});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertFalse(artists.isEmpty());
        Assert.assertEquals(3, artists.size());
    }

    /**
     * @since 18.12.17
     */
    @Test
    public void test018() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("createdAt__eq", new String[]{""});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertTrue(artists.isEmpty());
    }

    /**
     * @since 18.12.24
     */
    @Test
    public void test019() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("id__between", new String[]{"1,2"});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertFalse(artists.isEmpty());
        Assert.assertEquals(2, artists.size());
    }

    /**
     * @since 18.12.24
     */
    @Test
    public void test020() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("createdAt__between", new String[]{"1996,2000"});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertFalse(artists.isEmpty());
        Assert.assertEquals(2, artists.size());
    }

    /**
     * @since 18.12.24
     */
    @Test
    public void test021() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("createdAt__between", new String[]{",2000"});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertTrue(artists.isEmpty());
    }

    /**
     * @since 18.12.24
     */
    @Test
    public void test022() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("createdAt__between", new String[]{"1996"});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertTrue(artists.isEmpty());
    }

    /**
     * @since 18.12.24
     */
    @Test
    public void test023() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("createdAt__not__between", new String[]{"1996,2000"});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertFalse(artists.isEmpty());
        Assert.assertEquals(1, artists.size());
    }

    /**
     * @since 18.12.24
     */
    @Test
    public void test024() {
        final Map<String, String[]> args = new HashMap<>();
        args.put("name__between", new String[]{"A%,E%"});
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args);
        final List<Artist> artists = query.findList();

        Assert.assertFalse(artists.isEmpty());
        Assert.assertEquals(1, artists.size());
    }
}
