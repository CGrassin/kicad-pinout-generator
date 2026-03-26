#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sat Apr 25 10:14:42 2020

@author: charles
"""

import json
import os, mido, re

# =============ABC=====================
def abc2midi(abc_file, output_file, verbose=False):
    os.popen("abc2midi "+abc_file+" -o "+output_file + " -NGUI -NGRA").read()
    if verbose: print("Converted "+abc_file+" to MIDI")

# =============Files====================
def rmFile(file, verbose=False):
    if os.path.exists(file):
      os.remove(file)
    if verbose: print("Deleted "+file)
def sanitizeFileName(filename):
    return re.sub(r'[^a-zA-Z0-9_]', '', filename).lower()

#==============Music====================
halfHoled = [55,57,60,62,67,69,72,74,76]
dWhistle_min = 54
dWhistle_max = 78
def autoTranspose(notes_pitch, allow_half_holed=False):
    note_norest = [i for i in notes_pitch if i != 0]
    
    # If already in range:
    if (min(note_norest) >= dWhistle_min and max(note_norest) <= dWhistle_max and 
       (allow_half_holed or len([i for i in note_norest if i in halfHoled]) == 0)):
        return 0
     
    # Try simple octave up (should work most of the times)
    if (min(note_norest)+12 >= dWhistle_min and max(note_norest)+12 <= dWhistle_max and 
        (allow_half_holed or len([i for i in note_norest if i+12 in halfHoled]) == 0)):
        return 12
    
    # Automatic semi tone finder
    shift = 0
    while(max(note_norest)+shift <= dWhistle_max):
        if(min(note_norest)+shift >= dWhistle_min and max(note_norest)+shift <= dWhistle_max and 
           (allow_half_holed or len([i for i in note_norest if i+shift in halfHoled]) == 0)):
            print("Warning: shift is not perfect octave up (",shift,")")
            return shift
        shift += 1

    if not allow_half_holed:
        print("Warning: couldn't find a transposition without half holed notes, trying with...")
        return autoTranspose(notes_pitch,True)
    else:
        raise Exception("Couldn't transpose tune!")

offsetsD = ["D", "Db", "C", "B", "Bb", "A", "G#", "G", "F#",  "F", "E", "Eb"]
def getWhistle(shift):
    shift = (shift + 120)%12
    return offsetsD[shift]

# =============Output===================
def midi2string(midi_file):
    output = ""
    
    mid = mido.MidiFile(midi_file, clip=True)
    
    notes_key = []
    notes_dur = []
    tempo_mod = 1
    
    for msg in mid.tracks[0]:
        if msg.type == 'set_tempo': 
            tempo_mod = msg.tempo/500000
        elif msg.type == 'note_on' and msg.velocity != 0:
            if(msg.time > 1): # Ignore short rests
                notes_key.append(0);
                notes_dur.append(msg.time*tempo_mod);
        elif msg.type == 'note_off' or (msg.type == 'note_on' and msg.velocity == 0):
            notes_key.append(msg.note-20); # -20 for MIDI to key
            notes_dur.append(msg.time*tempo_mod);
    
    # Transpose to playable
    shift = autoTranspose(notes_key)
    
    # Make string
    for i in range(len(notes_key)):
        if notes_key[i] != 0: notes_key[i] += shift
        output += str(notes_key[i])+"/"+str(int(notes_dur[i]))+",";
        
    # remove trailing comma        
    output = output[:-1]
    return output, shift

def addDBentry(database, tune_title, tune_author, tune_type, tune_license, tune_abc,
               tune_sheetauthor, notes_filename, shift, isFirstTune):
    tune_title = tune_title.replace('"','\\"')
    if tune_title[-5:] ==', The': tune_title = 'The ' + tune_title[:-5]
    
    if not isFirstTune: database.write(',')
    database.write('\n{')
    database.write('"title":"'+tune_title+'",')
    if tune_author is not None:
        database.write('"author":"'+tune_author.replace('\n','').replace('"','\\"')+'",')
    database.write('"type":"'+tune_type.title()+'",')
    if tune_sheetauthor is not None: 
        database.write('"sheet_author":"'+tune_sheetauthor.replace('\n','').replace('"','\\"')+'",')
    if tune_license is not None: 
        database.write('"license":"'+tune_license.replace('\n','').replace('"','\\"')+'",')
    database.write('"whistle":"'+getWhistle(shift)+'",')
    database.write('"abc":"'+tune_abc.replace('\r\n','\n').replace('\r','\n').replace('\n','\\n').replace('"','\\"')+'",')
    database.write('"file":"'+notes_filename+'"')
    database.write('}')

def abc2notes(tune_abc):
    with open("tmp.abc", 'w') as content_file:
        content_file.write(tune_abc)
    abc2midi("tmp.abc","tmp.mid")
    rmFile("tmp.abc")
    tune_notes, shift = midi2string("tmp.mid")
    rmFile("tmp.mid")
    return tune_notes, shift

def writeNotes(tune_abc,tune_title, output_dir):
    print('Processing '+tune_title+'...')
    tune_notes, shift = abc2notes(tune_abc)
    notes_filename = "m_"+sanitizeFileName(tune_title)
    with open(output_dir + '/'+ notes_filename+".txt", 'w') as notes:
        notes.write(tune_notes)
    return notes_filename, shift

# =============Read files===================
def readABC(input_abc, isFirstTune, database, output_dir):
    abc_buffer = ''
    tune_title = ''
    tune_type = 'Misc.'
    tune_author = None 
    
    input_abc = open(input_abc)
    for line in input_abc:
        # Process header
        if (line[:2] == 'X:' and len(abc_buffer) > 0):
            notes_filename, shift = writeNotes(abc_buffer, tune_title, output_dir)
            addDBentry(database, tune_title, tune_author, tune_type, None, 
                   abc_buffer, None, notes_filename, shift, isFirstTune)
            abc_buffer = ''
            tune_title = ''
            tune_type = 'Misc.'
            tune_author = None
            isFirstTune = False
        elif line[:2] == 'T:': tune_title = line[2:].replace('\n','')
        elif line[:2] == 'C:': tune_author = line[2:].replace('\n','')
        elif line[:2] == 'R:': tune_type  = line[2:].replace('\n','').replace('"','\\"')
        
        abc_buffer += line

    notes_filename, shift = writeNotes(abc_buffer.replace("~",""), tune_title, output_dir) 
    addDBentry(database, tune_title, "Trad.", tune_type, None, abc_buffer, None, notes_filename, shift, isFirstTune)
    input_abc.close()
    
    return isFirstTune

def readJSON(input_json, isFirstTune, database, output_dir):
    # Read input
    with open(input_json, 'r') as content_file:
        input_json = json.loads(content_file.read())
    
    for tune in input_json:
        # Read attributes
        tune_title = tune['name']
        tune_author = tune['author'] if "author" in tune else None
        tune_type = tune['type'] if "type" in tune else "Misc."
        tune_license = tune['license'] if "license" in tune else "ODbL"
        tune_abc = tune['abc']
        tune_sheetauthor = tune['username'] if "username" in tune else None
        
        # Make ABC
        tune_abc = "T:" + tune_title + '\n' + tune_abc
        if "meter" in tune: tune_abc = "M:" + tune['meter'] + '\n' + tune_abc
        if "mode" in tune: tune_abc = "K:" + tune['mode'] + '\n' + tune_abc
        if tune_abc[0:2] != "X:": tune_abc = 'X:1\n' + tune_abc
    
        # Get notes
        notes_filename, shift = writeNotes(tune_abc.replace("~",""),tune_title, output_dir)
        
        # Write DB entry
        
        isFirstTune = False
        addDBentry(database, tune_title, tune_author, tune_type, tune_license, 
                   tune_abc,tune_sheetauthor, notes_filename, shift, isFirstTune)
    return isFirstTune
#==============================================

def main(input_json = "input.json", input_abc  = "input.abc", output_dir = "output"):
    output_file = output_dir + "/db.json"

    # Clean folder
    filelist = os.listdir(output_dir)
    for f in filelist:
        os.remove(os.path.join(output_dir, f))

    # Generate db
    with open(output_file, 'w') as database:
        isFirstTune = True
        
        database.write('[')
        
        isFirstTune = readABC(input_abc, isFirstTune, database, output_dir)
        isFirstTune = readJSON(input_json, isFirstTune, database, output_dir)

        database.write('\n]')

    # Sort json
    print('Sorting database...')
    with open(output_file, 'r') as database:
        database = json.load(database)
        data = sorted(database, key=lambda k: k['title'])

    with open(output_file, 'w') as database:
        json.dump(data, database,ensure_ascii=False)
    
    print('Done!')

if __name__ == "__main__":
    main()