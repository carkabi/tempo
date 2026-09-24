package com.cappielloantonio.tempo.util;

import com.cappielloantonio.tempo.subsonic.models.Genre;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class PeachGenreCatalogTest {

    @Test
    public void groupsHipHopAliasesTogether() {
        List<Genre> raw = new ArrayList<>();
        raw.add(genre("Rap", 120));
        raw.add(genre("Hip Hop", 80));
        raw.add(genre("Hip-Hop", 60));
        raw.add(genre("Gangsta Rap", 20));

        PeachGenreCatalog.Group group = find(
                PeachGenreCatalog.build(raw),
                "Rap & Hip-Hop"
        );

        Assert.assertNotNull(group);
        Assert.assertEquals(280, group.getSongCount());
        Assert.assertEquals(4, group.getRawGenres().size());
    }
    @Test
    public void groupsRnbAndSoulTogether() {
        List<Genre> raw = new ArrayList<>();
        raw.add(genre("R&B", 100));
        raw.add(genre("RnB", 40));
        raw.add(genre("Soul", 30));
        raw.add(genre("Neo Soul", 10));

        PeachGenreCatalog.Group group = find(
                PeachGenreCatalog.build(raw),
                "R&B & Soul"
        );

        Assert.assertNotNull(group);
        Assert.assertEquals(180, group.getSongCount());
    }

    @Test
    public void hidesMetadataPollution() {
        List<Genre> raw = new ArrayList<>();
        raw.add(genre("2019", 20));
        raw.add(genre("Unknown", 10));
        raw.add(genre("Female Vocalists", 8));
        raw.add(genre("Taking Chances Deluxe Digital Album", 5));

        Assert.assertTrue(
                PeachGenreCatalog.build(raw).isEmpty()
        );
    }
    @Test
    public void keepsCanonicalListLimited() {
        List<Genre> raw = new ArrayList<>();
        raw.add(genre("Pop", 100));
        raw.add(genre("Rock", 90));
        raw.add(genre("Techno", 80));
        raw.add(genre("Jazz", 70));
        raw.add(genre("Reggae", 60));
        raw.add(genre("Latin Pop", 50));
        raw.add(genre("Classique", 40));
        raw.add(genre("World Music", 30));

        List<PeachGenreCatalog.Group> groups =
                PeachGenreCatalog.build(raw);

        Assert.assertTrue(groups.size() <= 17);
    }

    private Genre genre(String name, int songs) {
        Genre genre = new Genre();
        genre.setGenre(name);
        genre.setSongCount(songs);
        return genre;
    }
    private PeachGenreCatalog.Group find(
            List<PeachGenreCatalog.Group> groups,
            String title
    ) {
        for (PeachGenreCatalog.Group group : groups) {
            if (title.equals(group.getTitle())) {
                return group;
            }
        }

        return null;
    }
}
