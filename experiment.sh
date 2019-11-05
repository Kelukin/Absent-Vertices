#java -cp ./bin/ Main
./build.sh
runJava="java -cp ./bin Main " 
fileDirPath=~/Downloads/datasets/dataFiles/
fileList=$(ls $fileDirPath)
flag=0
measure_time=0
measure_opt=""
loop="f"
if [ $# != 0 ]; then
	for z in $@
	do
		if [ $z == "--small" ]; then flag=1
		fi
		if [ $z == "--big" ]; then flag=2
		fi
		if [ $z == "-t" ]; then measure_opt=" -t "
		fi
		if [ $z == "-m" ]; then measure_opt=$measure_opt" -m "
		fi
		if [ $z == "-loop"]; then loop="t"
	done
fi
if [ $loop == "f" ]; then
	#clear the file
	echo " " > record.txt
	for i in $fileList
	do
		filePath=$fileDirPath$i
		tmp=$(du -b $filePath)
		fileSize=`expr ${tmp%$filePath}`
		if [ $fileSize -gt $((1024*1024*10)) ]  && [ $flag != 1 ]; then
			echo $filePath
			date
			echo $filePath >> record.txt
			$runJava $measure_opt $filePath >> record.txt
		fi
		if [ $(($fileSize)) -le $((1024*1024*10)) ] && [ $flag != 2 ]; then
			echo $filePath >> record.txt
			$runJava $measure_opt $filePath >> record.txt
		fi
	done
	newFileName=record_new.txt
	if [ $flag == 2 ]; then
		newFileName=recordBig.txt
	elif [ $flag == 1 ]; then
		newFileName=recordSmall.txt
	fi
	mv record.txt $newFileName
else
	for workMode in 0 1 2 3 4; do
		work_opt="-w "$workMode
		echo $work_opt
		outputFile=record_$workMode.txt
		touch $outputFile
		for i in $fileList
		do
			echo $i
			for times in 0 1 2; do
				filePath=$fileDirPath$i
				echo times: $times $filePath >> $outputFile
				$runJava $filePath $measure_opt $work_opt >> $outputFile
			done
		done
		newFileName=record_new$workMode.txt
		mv $outputFile $newFileName
	done
fi

