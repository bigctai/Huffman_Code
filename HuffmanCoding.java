package huffman;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class contains methods which, when used together, perform the
 * entire Huffman Coding encoding and decoding process
 * 
 * @author Ishaan Ivaturi
 * @author Prince Rawal
 */
public class HuffmanCoding {
    /**
     * Writes a given string of 1's and 0's to the given file byte by byte
     * and NOT as characters of 1 and 0 which take up 8 bits each
     * 
     * @param filename The file to write to (doesn't need to exist yet)
     * @param bitString The string of 1's and 0's to write to the file in bits
     */
    public static void writeBitString(String filename, String bitString) {
        byte[] bytes = new byte[bitString.length() / 8 + 1];
        int bytesIndex = 0, byteIndex = 0, currentByte = 0;

        // Pad the string with initial zeroes and then a one in order to bring
        // its length to a multiple of 8. When reading, the 1 signifies the
        // end of padding.
        int padding = 8 - (bitString.length() % 8);
        String pad = "";
        for (int i = 0; i < padding-1; i++) pad = pad + "0";
        pad = pad + "1";
        bitString = pad + bitString;

        // For every bit, add it to the right spot in the corresponding byte,
        // and store bytes in the array when finished
        for (char c : bitString.toCharArray()) {
            if (c != '1' && c != '0') {
                System.out.println("Invalid characters in bitstring");
                System.exit(1);
            }

            if (c == '1') currentByte += 1 << (7-byteIndex);
            byteIndex++;
            
            if (byteIndex == 8) {
                bytes[bytesIndex] = (byte) currentByte;
                bytesIndex++;
                currentByte = 0;
                byteIndex = 0;
            }
        }
        
        // Write the array of bytes to the provided file
        try {
            FileOutputStream out = new FileOutputStream(filename);
            out.write(bytes);
            out.close();
        }
        catch(Exception e) {
            System.err.println("Error when writing to file!");
        }
    }
    
    /**
     * Reads a given file byte by byte, and returns a string of 1's and 0's
     * representing the bits in the file
     * 
     * @param filename The encoded file to read from
     * @return String of 1's and 0's representing the bits in the file
     */
    public static String readBitString(String filename) {
        String bitString = "";
        
        try {
            FileInputStream in = new FileInputStream(filename);
            File file = new File(filename);

            byte bytes[] = new byte[(int) file.length()];
            in.read(bytes);
            in.close();
            
            // For each byte read, convert it to a binary string of length 8 and add it
            // to the bit string
            for (byte b : bytes) {
                bitString = bitString + 
                String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            }

            // Detect the first 1 signifying the end of padding, then remove the first few
            // characters, including the 1
            for (int i = 0; i < 8; i++) {
                if (bitString.charAt(i) == '1') return bitString.substring(i+1);
            }
            
            return bitString.substring(8);
        }
        catch(Exception e) {
            System.out.println("Error while reading file!");
            return "";
        }
    }

    /**
     * Reads a given text file character by character, and returns an arraylist
     * of CharFreq objects with frequency > 0, sorted by frequency
     * 
     * @param filename The text file to read from
     * @return Arraylist of CharFreq objects, sorted by frequency
     */
    public static ArrayList<CharFreq> makeSortedList(String filename) {
        StdIn.setFile(filename);
        ArrayList<CharFreq> characters = new ArrayList<CharFreq>();
        int[] arr = new int[128];
        int count1 = 0;
        while(StdIn.hasNextChar()){
            char c = StdIn.readChar();
            arr[c] = arr[c]+1;
            count1++;
        }
        int count = 0;
        for(int i = arr.length-1; i>=0; i--){
            if(arr[i]>0){
                CharFreq c = new CharFreq((char)i, (double)arr[i]/count1);
                characters.add(c);
                count++;
            }
        }
        if(count==1){
            int num = (int)characters.get(0).getCharacter();
            if(num==127){
                num=0;
            }
            else{
                num++;
            }
            CharFreq c = new CharFreq((char)num, 0);
            characters.add(1, c);
        }
        Collections.sort(characters);
        return characters;
    }

    /**
     * Uses a given sorted arraylist of CharFreq objects to build a huffman coding tree
     * 
     * @param sortedList The arraylist of CharFreq objects to build the tree from
     * @return A TreeNode representing the root of the huffman coding tree
     */
    public static TreeNode makeTree(ArrayList<CharFreq> sortedList) {
        Queue <TreeNode> source = new Queue <TreeNode>();
        Queue <TreeNode> target = new Queue <TreeNode>();
        for(CharFreq i : sortedList){
            TreeNode t = new TreeNode(i, null, null);
            source.enqueue(t);
        }
        TreeNode t0 = source.dequeue();
        TreeNode t00 = source.dequeue();
        CharFreq c = new CharFreq(null, t0.getData().getProbOccurrence()+t00.getData().getProbOccurrence());
        target.enqueue(new TreeNode(c, t0, t00));
        while(!source.isEmpty()||target.size()>1){
            TreeNode t1 = new TreeNode(null, null, null);
            TreeNode t2 = new TreeNode(null, null, null);
            if(!source.isEmpty()&&(source.peek().getData().getProbOccurrence())<=(target.peek().getData().getProbOccurrence())){
                t1 = source.dequeue();
                if(!source.isEmpty()&&(source.peek().getData().getProbOccurrence())<=(target.peek().getData().getProbOccurrence())){
                    t2 = source.dequeue();
                }
                else{
                    t2 = target.dequeue();
                }
            }
            else{
                t1 = target.dequeue();
                if(target.isEmpty()||!source.isEmpty()&&(source.peek().getData().getProbOccurrence())<=(target.peek().getData().getProbOccurrence())){
                    t2 = source.dequeue();
                }
                else{
                    t2 = target.dequeue();
                }
            }
            CharFreq c1 = new CharFreq(null, t1.getData().getProbOccurrence()+t2.getData().getProbOccurrence());
            target.enqueue(new TreeNode(c1, t1, t2));
        }
        return target.peek();
    }

    /**
     * Uses a given huffman coding tree to create a string array of size 128, where each
     * index in the array contains that ASCII character's bitstring encoding. Characters not
     * present in the huffman coding tree should have their spots in the array left null
     * 
     * @param root The root of the given huffman coding tree
     * @return Array of strings containing only 1's and 0's representing character encodings
     */
    
    public static String[] makeEncodings(TreeNode root) {
        String[] s = new String[128];
        String o = new String();
        traverse(root, o, s);
        return s;
    }

    /**
     * Using a given string array of encodings, a given text file, and a file name to encode into,
     * this method makes use of the writeBitString method to write the final encoding of 1's and
     * 0's to the encoded file.
     * 
     * @param encodings The array containing binary string encodings for each ASCII character
     * @param textFile The text file which is to be encoded
     * @param encodedFile The file name into which the text file is to be encoded
     */
    public static void encodeFromArray(String[] encodings, String textFile, String encodedFile) {
        StdIn.setFile(textFile);
        String s = new String();
        while(StdIn.hasNextChar()){
            s = s+encodings[StdIn.readChar()];
        }
        writeBitString(encodedFile, s);
    }
    
    /**
     * Using a given encoded file name and a huffman coding tree, this method makes use of the 
     * readBitString method to convert the file into a bit string, then decodes the bit string
     * using the tree, and writes it to a file.
     * 
     * @param encodedFile The file which contains the encoded text we want to decode
     * @param root The root of your Huffman Coding tree
     * @param decodedFile The file which you want to decode into
     */
    public static void decode(String encodedFile, TreeNode root, String decodedFile) {
        StdOut.setFile(decodedFile);
        String s = readBitString(encodedFile);
        String out = new String();
        TreeNode start = root;
        for(int i = 0; i<s.length(); i++){
            String ok = "";
            if(i==s.length()-1){
                ok = s.substring(i);
            }
            else{
                ok = s.substring(i, i+1); 
            }
            if(root.getData().getCharacter()!=null){
                out = out + root.getData().getCharacter();
                root = start;
                i--;
            } 
            else if(ok.equals("0")){
                root = root.getLeft();
                if(i==s.length()-1){
                    out = out + root.getData().getCharacter();
                }
            }
            else if(ok.equals("1")){
                root = root.getRight();
                if(i==s.length()-1){
                    out = out + root.getData().getCharacter();
                }
            }
        }
        StdOut.print(out);
    }

    private static void traverse(TreeNode root, String encoded, String[]a){
        if(root==null)return;
        if(root.getData().getCharacter()!=null){
            a[root.getData().getCharacter()] = encoded;
        }
        traverse(root.getLeft(), encoded+0, a);
        traverse(root.getRight(), encoded+1, a); 
    }
}
