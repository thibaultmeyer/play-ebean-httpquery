/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Thibault Meyer
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

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.Query;
import com.avaje.ebean.config.ServerConfig;
import com.zero_x_baadf00d.ebean.PlayEbeanHttpQuery;
import models.Album;
import models.Artist;
import models.Cover;
import org.avaje.agentloader.AgentLoader;
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
 * @version 16.09.06
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
        Tests.playEbeanHttpQuery.addAlias("boap", "album");
        Tests.playEbeanHttpQueryClone = (PlayEbeanHttpQuery) Tests.playEbeanHttpQuery.clone();


        if (!AgentLoader.loadAgentFromClasspath("avaje-ebeanorm-agent", "debug=1;packages=models.**")) {
            System.err.println("avaje-ebeanorm-agent not found in classpath - not dynamically loaded");
        }
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName("default");
        serverConfig.loadFromProperties();
        serverConfig.setDefaultServer(true);
        serverConfig.setRegister(true);
        Tests.ebeanServer = EbeanServerFactory.create(serverConfig);

        try {
            final Scanner scanner = new Scanner(Tests.class.getResourceAsStream("/data-test.txt"));
            while (scanner.hasNextLine()) {
                final String name = scanner.nextLine();
                Artist artist = Artist.find.where().ilike("name", name).findUnique();
                if (artist == null) {
                    artist = new Artist();
                    artist.setName(name);
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
        final Query<Artist> query = Tests.playEbeanHttpQuery.buildQuery(Artist.class, args, Tests.ebeanServer.createQuery(Artist.class));
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
        final Query<Cover> query = Tests.playEbeanHttpQuery.buildQuery(Cover.class, args, Tests.ebeanServer.createQuery(Cover.class));
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
        final Query<Cover> query = Tests.playEbeanHttpQueryClone.buildQuery(Cover.class, args, Tests.ebeanServer.createQuery(Cover.class));
        final List<Cover> covers = query.findList();

        Assert.assertEquals(3, covers.size());
        Assert.assertEquals("https://domain.local/cover/123897948989", covers.get(0).getUrl());
        Assert.assertEquals("https://domain.local/cover/298372983720", covers.get(1).getUrl());
        Assert.assertEquals("https://domain.local/cover/898656564654", covers.get(2).getUrl());
    }
}
