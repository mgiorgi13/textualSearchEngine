package it.unipi.dii.mircv.index.preprocessing;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import ca.rmen.porterstemmer.PorterStemmer;
import it.unipi.dii.mircv.index.structures.Document;

/**
 * The Preprocessing class handles the preprocessing of text data, including tokenization,
 * removal of stopwords, and optional Porter stemming.
 */
public class Preprocessing {
    private static final String STOPWORDS_PATH = "data/stop_words_english.txt";
    private Document doc;
    public List<String> tokens = new ArrayList<>();
    private List<String> stopwords = new ArrayList<>();

    /**
     * Constructs a Preprocessing object for processing a document or query string.
     * Loads the list of stopwords from a predefined file.
     */
    public Preprocessing() {
        this.stopwords = getStopwords();
    }

    /**
     * Constructs a Preprocessing object for processing a query string.
     *
     * @param query               The query string to be preprocessed.
     * @param porterStemmerOption A boolean indicating whether Porter stemming should be applied.
     */
    public void queryPreprocess(String query, boolean porterStemmerOption) {
        List<String> words = tokenization(query);
        words = removeNumbers(words); // Remove words that contain more than 4 digits
        words = removeWordstop(words); // Remove stopwords
        if (porterStemmerOption) {
            PorterStemmer porterStemmer = new PorterStemmer(); // Stemming

            this.tokens = words.stream()
                    .map(porterStemmer::stemWord) // Apply stemming to each word
                    .collect(Collectors.toList());
        } else {
            this.tokens = words;
        }
    }

    /**
     * Constructs a Preprocessing object for processing a document.
     *
     * @param document            The document content to be preprocessed.
     * @param docCounter          The document counter.
     * @param porterStemmerOption A boolean indicating whether Porter stemming should be applied.
     */
    public void documentPreprocess(String document, int docCounter, boolean porterStemmerOption) {
        // create new document
        this.doc = new Document(document, docCounter);
        List<String> words = tokenization(doc.getBody());
        words = removeNumbers(words); // Remove words that contain numbers
        words = removeWordstop(words); // Remove stopwords
        if (porterStemmerOption) {
            PorterStemmer porterStemmer = new PorterStemmer(); // Stemming
            List<String> stemWords = words.stream()
                    .map(porterStemmer::stemWord) // Apply stemming to each word
                    .collect(Collectors.toList());

            this.doc.setLength(stemWords.size());
            this.tokens = stemWords;
        } else {
            this.doc.setLength(words.size());
            this.tokens = words;
        }
    }

    /**
     * Removes words from the list that contain more than 4 digits.
     *
     * @param words The list of words to be processed.
     * @return A list of words with numeric words removed.
     */
    public List<String> removeNumbers(List<String> words) {
        List<String> filteredWords = words.stream()
                .filter(word -> !word.matches(".*\\d.*"))
                .collect(Collectors.toList());
        return filteredWords;
    }

    /**
     * Tokenizes the input document into a list of words.
     *
     * @param doc The document content to be tokenized.
     * @return A list of words extracted from the document.
     */
    public List<String> tokenization(String doc) {
        String regex = "\\s+|\\!|\"|\\#|\\$|\\%|\\&|\\'|\\(|\\)|\\*|\\+|"
                + "\\,|\\-|\\.|\\/|\\:|\\;|\\<|\\=|\\>|\\|\\?|\\@|\\[|"
                + "\\]|\\^|\\`|\\{|\\||\\}|\\~|\\\\|\\_|[\\s!\"#$%&'()*+,\\-./:;<=>?@\\[\\]^`{|}~]+";
        Pattern pattern = Pattern.compile(regex);

        List<String> words = pattern.splitAsStream(doc.toLowerCase())
                .flatMap(subToken -> Arrays.stream(subToken.split(regex)))
                .filter(subToken -> !subToken.isEmpty())
                .collect(Collectors.toList());

        return words;
    }

    /**
     * Retrieves the list of stopwords from a predefined file.
     *
     * @return A list of stopwords.
     */
    private List<String> getStopwords() {
        List<String> stopwords = new ArrayList<>();

        try (BufferedReader file = new BufferedReader(new FileReader(STOPWORDS_PATH))) {
            String line;
            while ((line = file.readLine()) != null) {
                stopwords.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stopwords;
    }

    /**
     * Removes stopwords from a list of words.
     *
     * @param words The list of words to be processed.
     * @return A list of words with stopwords removed.
     */
    private List<String> removeWordstop(List<String> words) {
        // Use Java Streams to filter out stopwords
        List<String> filteredWords = words.stream()
                .filter(word -> !this.stopwords.contains(word))
                .collect(Collectors.toList());
        return filteredWords;
    }


    /**
     * Retrieves the preprocessed document.
     *
     * @return The preprocessed document.
     */
    public Document getDoc() {
        return doc;
    }
}
