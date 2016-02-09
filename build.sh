#!/bin/bash
for i in $(find -name '*.java'); do
	echo "Lips processing: " $i
	lips < $i | astyle > ../src/brya3525/$(basename $i)
done
cd ..
ant spacesettlers-human
