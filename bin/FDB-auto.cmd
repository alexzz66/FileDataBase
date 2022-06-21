@echo off
start /b /high java -cp %~d0%~p0 alexnick.filedatabase.FileDataBase -a:autoNoExtract;bigSize;plCrcNo42;plCrcYes;temp;tempYesX;finalPause;extractSaveYes;viewnomark42;compareTwoBinNoFullpaths %*
pause
:start /b /high :cd /d %~d0%~p0
:java -cp %~d0%~p0  >> set '-cp %~d0%~p0' on error reading files of "FileDataBase"

:FOR ALL MODES >>
:option register (bigsize,bigSize,bIGSIZE) no matter; all option must be separated by ";", example -a:tempYes;bigsize;finalPause
:"temp;" if defined, set mode 'TEMP', after confirm. In this mode be cleared extension options and bin-file be saved to 'temp' repository
:"tempYes;" the same as 'temp'; but without confirmation
:"finalPause;" if defined, in finished program, on console be written 'Final pause' and waiting press 'Enter' to close console
:"id3Confirm;" select on console id3New/id3All/id3No
:"id3No;" if not defined 'id3Confirm', set 'id3No'. NB: by default, set 'id3New'
:"bigSize;" for textFields, if defined, fields in several windows, will be increased on the 30%

:FOR -1 >>
:все, что относится к PathsListTable;
:"modeOnePlYes;" will be open 'PathsListTable' without confirmation; else need select: 'save to file' or 'PathsListTable'

:FOR -4: ('CompareFolders' call also from 'ViewTable', but without copying) >>
:"bak;" for copying files in 'backUpCopyFiles' (calls from 'CompareFolders' when 'stop_mode' == 4), writes on console 'SET>>', if defined

:FOR PathListTable >>
:"plCrcYes;" ";plCrcNo;" if one only defined, be or not be calculate crc in 'PathsListTable', column 'Signature'
:"plSingleOnly;" ";plSetMulti;" ";plSetDrag;" -> defines components settings on start show 'PathsListTable'
:"plOneFileYes;" "plOneFileNo;" if ONE parameter and not directory -> by default be confirmation, need read paths from this, or show in table; You may define answer
:"plOneFolderYes;" "plOneFolderNo;" if ONE parameter and it's directory -> by default be confirmation, search in this folder (analog mode '-1' with setting 'modeOnePlYes'), or show in table; You may define answer
:"replaceNoSubstringError;" when select 'replaceSourceDest...' for rename file, and no found 'source' in 'name', be set as rename error

:FOR -v (View mode) >>
:"viewnoid3;" if defined, while mode 'view', not be showed information in table column 'BinFolder, ID3, mark'
:"viewnomark;" if defined, while mode 'view', not be showed information in table column 'BinFolder, ID3, mark', and will not be button 'mark' for set mark
:"compareTwoBinNoFullpaths;" by default, comparing folders on click 'CompareTwoBin', will be generated file with full paths (with adding start path); if this option defined, start path won't be added

:FOR -e (-extract) >>
:"extractSaveYes;" if defined, after extracting be saving result without confirmation

:-a (-auto),-v (-view),-1,-2,-3,-4,-p (-pl, -pathsList), -e (-extract)

:mode '-a' : no parameters, set mode '-v'; else if one parameter and it folder -> mode '-4'; if file -> '-p'; if more than 1 parameter, mode '-e'
:if defined "autoNoExtract;" >> instead of 'extract mode' be set 'pathsList mode'
