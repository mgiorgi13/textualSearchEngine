package it.unipi.dii.mircv.index.structures;

import it.unipi.dii.mircv.index.utility.Logs;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PostingList{
    private ArrayList<Posting> postings;
    int size;

    Logs log = new Logs();

    public PostingList(Document doc) {
        postings = new ArrayList<>();
        postings.add(new Posting(doc.getDocID(), 1));
        size = 1;
    }

    public PostingList() {
        postings = new ArrayList<>();
        size = 0;
    }

    public ArrayList<Posting> getPostings() {
        return postings;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("[");
        for (int i = 0; i < postings.size(); i++) {
            Posting posting = postings.get(i);
            int docID = posting.getDocID();
            int freq = posting.getFreq();

            output.append("(").append(docID).append(", ").append(freq).append(")");

            if (i < postings.size() - 1) {
                output.append(" -> ");
            }
        }
        output.append("]\n");
        //System.out.println(output);
        //System.out.println("**************************************");
        return output.toString();
    }

    // binary search on posting list to find the document return the posting
    public void updatePostingList(Document doc) {
        int docID = doc.getDocID();
        int low = 0;
        int high = this.postings.size() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            Posting posting = this.postings.get(mid);
            int postingDocID = posting.getDocID();

            if (postingDocID == docID) {
                posting.updateFreq();
                return;
            } else if (postingDocID < docID) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        // posting list doesn't contain the document, create new posting
        Posting newPosting = new Posting(doc.getDocID(), 1); // create new posting
        this.postings.add(newPosting); // add posting to posting list
        this.size++;
    }

    public int getPostingListSize() {
        return this.size;
    }

    public long savePostingListToDisk(int indexCounter)  {
        String filePath = "data/index/index_" + indexCounter + ".bin";

        long offset = -1;

        try {

            RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw");
            // Posizionati alla fine del file per l'aggiunta dei dati
            randomAccessFile.seek(randomAccessFile.length());

            // Memorizza la posizione di inizio nel file
            offset = randomAccessFile.getFilePointer();
//            System.out.println("Initial offset: " + offset); // Debug: Stampa l'offset

            for (Posting posting : this.postings) {
                randomAccessFile.writeInt(posting.getDocID());
                randomAccessFile.writeInt(posting.getFreq());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return offset;
    }

    public ArrayList<Posting> readPostingList(int indexCounter, int df, long offset) {
        String filePath = "data/index/index_" + indexCounter + ".bin";
        ArrayList<Posting> result = new ArrayList<>();

        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "r");

//            System.out.println("File path: " + filePath); // Debug: Stampa il percorso del file
//            System.out.println("Offset: " + offset); // Debug: Stampa l'offset

            // Posizionati nella posizione desiderata
            randomAccessFile.seek(offset);
//            System.out.println("Size: " + df); // Debug: Stampa la dimensione della posting list

            for(int i = 0; i < df; i++) {
                int docID = randomAccessFile.readInt();
                int freq = randomAccessFile.readInt();
                result.add(new Posting(docID, freq));
            }

            randomAccessFile.close();


//            System.out.println("Dimensione della PostingList: " + result.size());
//            System.out.println("PostingList letta, docID e freq " + result.get(0).getDocID() + ", " + result.get(0).getFreq());

        } catch (IOException e) {
            e.printStackTrace();
        }

        this.postings = result;
        this.size = df;

        return result; // serve forse dopo per ricostruire l'indice
    }



}