find -name "*.java" > sources.txt
javac @sources.txt
java src/test/Test.java -sa
pkill -f 'java -jar'