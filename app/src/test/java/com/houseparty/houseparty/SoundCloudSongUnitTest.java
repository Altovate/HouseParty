package com.houseparty.houseparty;

import org.junit.Before;
import org.junit.Test;

public class SoundCloudSongUnitTest {
    private Song song;

    @Before
    public void setUp() {
        song = new SoundCloudSong("Beat It", "fake_uri", "Michael Jackson");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlaySong() {
        song.playSong();
    }
}