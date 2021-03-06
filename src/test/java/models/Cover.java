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
package models;

import io.ebean.Finder;
import io.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * Cover.
 *
 * @author Thibault Meyer
 * @version 16.12.20
 * @since 16.04.22
 */
@Entity
public class Cover extends Model {

    public static final Finder<Long, Cover> find = new Finder<>(Cover.class);

    @Id
    private Long id;

    private String url;

    @OneToOne(mappedBy = "cover")
    private Album album;

    public Long getId() {
        return this.id;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Album getAlbum() {
        return this.album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }
}
