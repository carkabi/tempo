package com.cappielloantonio.tempo.util;

import com.cappielloantonio.tempo.subsonic.models.Genre;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PeachGenreCatalog {
    private PeachGenreCatalog() {
    }

    public static final class Group {
        private final String title;
        private final ArrayList<String> rawGenres = new ArrayList<>();
        private int songCount;
        private int albumCount;

        Group(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
        public ArrayList<String> getRawGenres() {
            return new ArrayList<>(rawGenres);
        }

        public int getSongCount() {
            return songCount;
        }

        public int getAlbumCount() {
            return albumCount;
        }

        void add(Genre genre) {
            if (genre == null || genre.getGenre() == null) {
                return;
            }

            rawGenres.add(genre.getGenre());
            songCount += genre.getSongCount();
            albumCount += genre.getAlbumCount();
        }
    }

    public static List<Group> build(List<Genre> genres) {
        Map<String, Group> groups = new LinkedHashMap<>();

        for (String title : canonicalTitles()) {
            groups.put(title, new Group(title));
        }
        if (genres != null) {
            for (Genre genre : genres) {
                if (genre == null || genre.getGenre() == null) {
                    continue;
                }

                String category = classify(genre.getGenre());
                Group group = groups.get(category);

                if (group != null) {
                    group.add(genre);
                }
            }
        }

        List<Group> result = new ArrayList<>();

        for (Group group : groups.values()) {
            if (!group.rawGenres.isEmpty()) {
                result.add(group);
            }
        }

        result.sort(
                Comparator.comparingInt(Group::getSongCount)
                        .reversed()
                        .thenComparing(Group::getTitle)
        );

        return result;
    }
    private static List<String> canonicalTitles() {
        List<String> titles = new ArrayList<>();
        titles.add("Pop");
        titles.add("Rock");
        titles.add("Rap & Hip-Hop");
        titles.add("R&B & Soul");
        titles.add("Dance");
        titles.add("Électro");
        titles.add("Funk & Disco");
        titles.add("Musiques du monde");
        titles.add("Chanson française");
        titles.add("House & Techno");
        titles.add("Reggae & Dancehall");
        titles.add("Bandes originales");
        titles.add("Latino");
        titles.add("Jazz & Blues");
        titles.add("Classique");
        titles.add("Folk & Country");
        titles.add("Ambiance & Chill");
        return titles;
    }

    private static String classify(String rawGenre) {
        String value = normalize(rawGenre);

        if (value.isEmpty() || isJunk(value)) {
            return null;
        }

        if (containsAny(value,
                "soundtrack", "film score", "bandes originales",
                "b o f", "musicals", "jeux video")
                || value.equals("film")
                || value.equals("films")
                || value.equals("games")) {
            return "Bandes originales";
        }
        if (containsAny(value,
                "rap", "hip hop", "hiphop", "gangsta",
                "hyphy", "grime", "trap", "conscious")) {
            return "Rap & Hip-Hop";
        }

        if (containsAny(value,
                "r and b", "r b", "rnb", "r amp",
                "soul", "neo soul", "contemporary r")) {
            return "R&B & Soul";
        }

        if (containsAny(value,
                "chanson francaise", "variete francaise",
                "chanson francais", "chanson")) {
            return "Chanson française";
        }

        if (containsAny(value,
                "reggae", "reggea", "raggae", "ragga",
                "dancehall", "dance hall", "ska",
                "rocksteady")) {
            return "Reggae & Dancehall";
        }

        if (containsAny(value,
                "latin", "latino", "reggaeton",
                "urban latino", "musique bresilienne",
                "guitarra espanola")) {
            return "Latino";
        }
        if (containsAny(value,
                "world", "musiques du monde",
                "musique africaine", "afrique",
                "soca music", "gospel")) {
            return "Musiques du monde";
        }

        if (containsAny(value,
                "jazz", "blues", "swing")) {
            return "Jazz & Blues";
        }

        if (containsAny(value,
                "classique", "classical", "chamber")) {
            return "Classique";
        }

        if (containsAny(value,
                "folk", "country", "acoustic",
                "singer songwriter")) {
            return "Folk & Country";
        }

        if (containsAny(value,
                "ambient", "ambiance", "downtempo",
                "new age", "lounge", "holiday",
                "christmas", "noel")) {
            return "Ambiance & Chill";
        }
        if (containsAny(value,
                "house", "techno", "trance",
                "circuit", "rave", "jumpstyle")) {
            return "House & Techno";
        }

        if (containsAny(value,
                "electro", "electronic", "electronique",
                "edm", "electronica", "synth",
                "industrial", "trip hop", "laptop",
                "broken beat", "dubstep", "abstract")) {
            return "Électro";
        }

        if (containsAny(value,
                "dance", "eurodance", "hi nrg",
                "eurodisco")) {
            return "Dance";
        }

        if (containsAny(value,
                "funk", "disco", "doo wop")) {
            return "Funk & Disco";
        }

        if (containsAny(value,
                "rock", "grunge", "punk",
                "new wave")) {
            return "Rock";
        }
        if (containsAny(value,
                "pop", "indie", "alternative",
                "alternatif", "ballad", "top 40",
                "variete internationale")) {
            return "Pop";
        }

        return null;
    }

    private static boolean isJunk(String value) {
        if (value.matches("\\d{4}")
                || value.matches("\\d{2}s")) {
            return true;
        }

        return containsAny(value,
                "unknown", "other", "divers",
                "female vocalist", "i have this album",
                "album collection", "taking chances",
                "sonshub", "diction", "mixtape",
                "albuns que eu tenho")
                || value.equals("female")
                || value.equals("singer")
                || value.equals("songwriter")
                || value.equals("genre")
                || value.equals("french")
                || value.equals("canadian")
                || value.equals("vf");
    }
    private static boolean containsAny(
            String value,
            String... needles
    ) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }

        return false;
    }

    private static String normalize(String value) {
        String normalized = Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        );

        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = normalized
                .toLowerCase(Locale.ROOT)
                .replace("&", " and ")
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");

        return normalized;
    }
}
