package com.unique.simplealarmclock.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuoteManager {
    private static final List<String> quotes = new ArrayList<String>() {{
        // Time-related quotes
        add("The future depends on what you do today. - Mahatma Gandhi");
        add("Time is what we want most, but what we use worst. - William Penn");
        add("Lost time is never found again. - Benjamin Franklin");
        add("Time and tide wait for no man. - Geoffrey Chaucer");
        
        // Wake-up related quotes
        add("Wake up with determination, go to bed with satisfaction.");
        add("The early bird catches the worm.");
        add("Each morning we are born again. What we do today matters most. - Buddha");
        add("Morning is an important time of day, because how you spend your morning can often tell you what kind of day you are going to have.");
        
        // Consistency related quotes
        add("Success isn't always about greatness. It's about consistency. - Dwayne Johnson");
        add("Small daily improvements are the key to staggering long-term results.");
        add("Consistency is what transforms average into excellence.");
        add("It's not what we do once in a while that shapes our lives, but what we do consistently.");
        
        // Motivational quotes
        add("The only way to do great work is to love what you do. - Steve Jobs");
        add("Your future is created by what you do today, not tomorrow.");
        add("Don't watch the clock; do what it does. Keep going. - Sam Levenson");
        add("The difference between ordinary and extraordinary is that little extra.");
    }};

    public static String getRandomQuote() {
        Random random = new Random();
        return quotes.get(random.nextInt(quotes.size()));
    }
} 