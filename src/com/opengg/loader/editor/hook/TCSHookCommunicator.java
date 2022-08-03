package com.opengg.loader.editor.hook;

import com.opengg.core.Configuration;
import com.opengg.core.console.GGConsole;
import com.opengg.core.math.Vector3f;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class TCSHookCommunicator {
    private WinNT.HANDLE process;
    private String executablePath;
    private String directory;

    private int currentPlayerOneAddress;
    private int currentPlayerTwoAddress;
    private int mapAddress;

    private Map<String, Integer> addresses = new HashMap<>();

    public boolean attemptHook() {
        Tlhelp32.PROCESSENTRY32 entry = new Tlhelp32.PROCESSENTRY32();
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, null);
        if(Kernel32.INSTANCE.Process32First(snapshot, entry)){
            while (Kernel32.INSTANCE.Process32Next(snapshot, entry)){
                if(new String(entry.szExeFile).trim().equals(Configuration.get("hook-executable-name"))){
                    WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_ALL_ACCESS, false, entry.th32ProcessID.intValue());
                    if(process != null){
                        Memory characterPosValue = new Memory(Kernel32.MAX_PATH);
                        int len = Psapi.INSTANCE.GetModuleFileNameEx(process, null, characterPosValue, Kernel32.MAX_PATH);
                        executablePath = characterPosValue.getWideString(0);
                        directory = new File(executablePath).getParent();

                        GGConsole.log("Hooked into " + executablePath);

                        this.process = process;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean checkForValidityAndReaquirePointers(){
        if(Kernel32.INSTANCE.WaitForSingleObject(process, 0) != WinError.WAIT_TIMEOUT){
            return false;
        }

        Pointer pointer = Pointer.createConstant(0x93d810);
        Memory playerOneStructPtr = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, pointer, playerOneStructPtr, 4, null);
        currentPlayerOneAddress = playerOneStructPtr.getInt(0);

        Pointer pointer2 = Pointer.createConstant(0x93d814);
        Memory playerTwoStructPtr = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, pointer2, playerTwoStructPtr, 4, null);
        currentPlayerTwoAddress = playerTwoStructPtr.getInt(0);

        Pointer pointer3 = Pointer.createConstant(0x802c54);
        Memory mapPtr = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, pointer3, mapPtr, 4, null);
        mapAddress = mapPtr.getInt(0);

        return true;
    }
    public boolean checkForValidityAndReaquirePointersLIJ(){
        if(Kernel32.INSTANCE.WaitForSingleObject(process, 0) != WinError.WAIT_TIMEOUT){
            return false;
        }

        Pointer pointer = Pointer.createConstant(0xaae1f0);
        Memory playerOneStructPtr = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, pointer, playerOneStructPtr, 4, null);
        currentPlayerOneAddress = playerOneStructPtr.getInt(0);

        Pointer pointer2 = Pointer.createConstant(0xaae1f4);
        Memory playerTwoStructPtr = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, pointer2, playerTwoStructPtr, 4, null);
        currentPlayerTwoAddress = playerTwoStructPtr.getInt(0);

        Pointer pointer3 = Pointer.createConstant(0x933e94);
        Memory mapPtr = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, pointer3, mapPtr, 4, null);
        mapAddress = mapPtr.getInt(0);

        return true;
    }

    public void close(){
        Kernel32.INSTANCE.CloseHandle(process);
    }


    public Vector3f readVector3f(int address){
        Pointer vecPtr = new Pointer(address);
        Memory vecMem = new Memory(12);
        Kernel32.INSTANCE.ReadProcessMemory(process, vecPtr, vecMem, 12, null);

        return new Vector3f(vecMem.getFloat(0), vecMem.getFloat(4), vecMem.getFloat(8));
    }

    public void writeVector3f(int addr, Vector3f vector){
        Pointer vecPtr = new Pointer(addr);
        Memory vecMem = new Memory(12);
        vecMem.setFloat(0, vector.x);
        vecMem.setFloat(4, vector.y);
        vecMem.setFloat(8, vector.z);

        Kernel32.INSTANCE.WriteProcessMemory(process, vecPtr, vecMem, 12, null);

    }

    public void writeDebugOut(){
        Pointer vecPtr = new Pointer(0x9739d0);
        Memory vecMem = new Memory(4);
        vecMem.setInt(0, 2);

        Kernel32.INSTANCE.WriteProcessMemory(process, vecPtr, vecMem, 4, null);
    }

    public Vector3f readPlayerLocation(int player){
        return readCharacterLocation((player == 1 ? currentPlayerOneAddress : currentPlayerTwoAddress));
    }

    public Vector3f readCharacterLocation(int character){
        return readVector3f(character + 0x5c);
    }

    public float readPlayerAngle(int player){
        return readCharacterAngle((player == 1 ? currentPlayerOneAddress : currentPlayerTwoAddress));
    }

    public float readCharacterAngle(int character){
        Pointer characterAngle = new Pointer(character + 0x58);
        Memory characterAngleValue = new Memory(2);
        Kernel32.INSTANCE.ReadProcessMemory(process, characterAngle, characterAngleValue, 2, null);

        return (characterAngleValue.getShort(0) / 65535f) * 360;
    }

    public void teleportToPosition(int player, Vector3f pos) {
        writeVector3f((player == 1 ? currentPlayerOneAddress : currentPlayerTwoAddress) + 0x5c, pos);
    }

    public String getCurrentMap(){
        Pointer mapNameAddr = new Pointer(mapAddress);
        Memory mapName = new Memory(128);
        Kernel32.INSTANCE.ReadProcessMemory(process, mapNameAddr, mapName, 128, null);

        return new String(mapName.getByteArray(0, 128)).trim();
    }

    public List<TCSHookPanel.AIMessage> getAIMessages(){
        Memory mem = new Memory(8);
        Pointer pointer2 = new Pointer(0x95df5c+0x10);
        if(!Kernel32.INSTANCE.ReadProcessMemory(process, pointer2, mem, 8, null)){
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
        int entryPTR = mem.getInt(0);
        pointer2 = new Pointer(entryPTR);
        if(!Kernel32.INSTANCE.ReadProcessMemory(process, pointer2, mem, 8, null)){
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
        entryPTR = mem.getInt(4);
        ArrayList<TCSHookPanel.AIMessage> list = new ArrayList<>();
        while(entryPTR != 0){
            //System.out.println("Entry: " + Integer.toHexString(entryPTR));
            Pointer entryPointer = new Pointer(entryPTR);
            Memory entry = new Memory(56);
            if(!Kernel32.INSTANCE.ReadProcessMemory(process,entryPointer,entry,56,null)){
                throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
            }
            int next = entry.getInt(0);
            String name = new String(entry.getByteArray(8,32)).trim();
            float value = entry.getFloat(40);
            //System.out.println(name + "," + value);
            list.add(new TCSHookPanel.AIMessage(name,value,entryPTR));
            entryPTR = next;
        }
        return list;
    }
    public List<TCSHookPanel.AIMessage> getAIMessagesLIJ(){
        Memory mem2 = new Memory(4);
        Pointer pointer3 = new Pointer(0xad25b4);
        if(!Kernel32.INSTANCE.ReadProcessMemory(process, pointer3, mem2, 8, null)){
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
        Memory mem = new Memory(8);
        Pointer pointer2 = new Pointer(mem2.getInt(0)+0x10);
        if(!Kernel32.INSTANCE.ReadProcessMemory(process, pointer2, mem, 8, null)){
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
        int entryPTR = mem.getInt(0);
        pointer2 = new Pointer(entryPTR);
        if(!Kernel32.INSTANCE.ReadProcessMemory(process, pointer2, mem, 8, null)){
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
        entryPTR = mem.getInt(0);
        ArrayList<TCSHookPanel.AIMessage> list = new ArrayList<>();
        while(entryPTR != 0){
            //System.out.println("Entry: " + Integer.toHexString(entryPTR));
            Pointer entryPointer = new Pointer(entryPTR);
            Memory entry = new Memory(56);
            if(!Kernel32.INSTANCE.ReadProcessMemory(process,entryPointer,entry,56,null)){
                throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
            }
            int next = entry.getInt(0);
            String name = new String(entry.getByteArray(8,32)).trim();
            float value = entry.getFloat(40);
            //System.out.println(name + "," + value);
            list.add(new TCSHookPanel.AIMessage(name,value,entryPTR));
            entryPTR = next;
        }
        return list;
    }

    public void updateAIMessage(List<TCSHookPanel.AIMessage> messages){
        Memory valueMem = new Memory(4);
        Pointer valuePTR;
        for(TCSHookPanel.AIMessage message : messages){
            valuePTR = new Pointer(message.address + 40);
            valueMem.setFloat(0,message.value);
            Kernel32.INSTANCE.WriteProcessMemory(process, valuePTR, valueMem, 4, null);
        }
    }

    public void setTargetMap(int id){
        Pointer levelDataStartPtr = new Pointer(0x00951b98);//0x00951b78, 0x00951b98
        Memory levelDataStart = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, levelDataStartPtr, levelDataStart, 4, null);

        var end = id * 0x130 + levelDataStart.getInt(0);

        Memory newTarget = new Memory(4);
        newTarget.setInt(0, end);
        Pointer newLevelDataPtr = new Pointer(0x00951ba0);//0x00951b80, 0x00951ba0
        Kernel32.INSTANCE.WriteProcessMemory(process, newLevelDataPtr, newTarget, 4, null);

        Memory enableFlag = new Memory(4);
        enableFlag.setInt(0, 1);
        Pointer loadLevelPtr = new Pointer(0x0093d870);//0x0093d84c, 0x0093d870

        Kernel32.INSTANCE.WriteProcessMemory(process, loadLevelPtr, enableFlag, 4, null);
    }
    public void resetDoor(){
        Memory newTarget = new Memory(1);
        newTarget.setByte(0, (byte)0);
        Pointer newLevelDataPtr = new Pointer(0x009513d8);
        Kernel32.INSTANCE.WriteProcessMemory(process, newLevelDataPtr, newTarget, 1, null);
    }
    public void setResetBit(){
        Memory newTarget2 = new Memory(4);
        newTarget2.setInt(0, -1);
        Pointer newLevelDataPtr2 = new Pointer(0x803784);
        Kernel32.INSTANCE.WriteProcessMemory(process, newLevelDataPtr2, newTarget2, 4, null);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Memory newTarget = new Memory(4);
        newTarget.setInt(0, 32);
        Pointer newLevelDataPtr = new Pointer(0x0093b2cc);
        Kernel32.INSTANCE.WriteProcessMemory(process, newLevelDataPtr, newTarget, 4, null);
    }

    public void resetDoorLIJ(){
        Memory newTarget = new Memory(1);
        newTarget.setByte(0, (byte)0);
        Pointer newLevelDataPtr = new Pointer(0x00accd90);
        Kernel32.INSTANCE.WriteProcessMemory(process, newLevelDataPtr, newTarget, 1, null);
    }
    public void setResetLIJ(){
        Memory newTarget2 = new Memory(4);
        newTarget2.setInt(0, -1);
        Pointer newLevelDataPtr2 = new Pointer(0x9367e4);
        Kernel32.INSTANCE.WriteProcessMemory(process, newLevelDataPtr2, newTarget2, 4, null);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /*Memory newTarget3 = new Memory(4);
        newTarget2.setInt(0, -1);
        Pointer rbitptr = new Pointer(0xaab277);
        Kernel32.INSTANCE.WriteProcessMemory(process, rbitptr, newTarget3, 4, null);*/
        Memory newTarget = new Memory(4);
        newTarget.setInt(0, 1);
        Pointer newLevelDataPtr = new Pointer(0xacc7a8);
        Kernel32.INSTANCE.WriteProcessMemory(process, newLevelDataPtr, newTarget, 4, null);
    }
    public void setTargetMapLIJ(int id){
        Pointer levelDataStartPtr = new Pointer(0xacc91c);//0x00951b78, 0x00951b98
        Memory levelDataStart = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, levelDataStartPtr, levelDataStart, 4, null);

        var end = id * 0x148 + levelDataStart.getInt(0);

        Memory newTarget = new Memory(4);
        newTarget.setInt(0, end);
        Pointer newLevelDataPtr = new Pointer(0xacc924);//0x00951b80, 0x00951ba0
        Kernel32.INSTANCE.WriteProcessMemory(process, newLevelDataPtr, newTarget, 4, null);

        Memory enableFlag = new Memory(4);
        enableFlag.setInt(0, 1);
        Pointer loadLevelPtr = new Pointer(0xaae263);//0x0093d84c, 0x0093d870

        Kernel32.INSTANCE.WriteProcessMemory(process, loadLevelPtr, enableFlag, 4, null);
    }


    public Vector3f getCameraPosition(){
        Pointer camPtr = new Pointer(0x008021b4);
        Memory camStart = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, camPtr, camStart, 4, null);

        int camPos = camStart.getInt(0) + 0xe8;

        return readVector3f(camPos);
    }

    public Vector3f getCameraAngle(){
        Pointer camPtr = new Pointer(0x008021b4);
        Memory camStart = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, camPtr, camStart, 4, null);

        int camAngle = camStart.getInt(0) + 0x214;
        var orientation = readVector3f(camAngle);
        return orientation;
        // return new Vector3f(orientation.x, orientation.y, 0);
    }

    public String encodeHexString(byte[] byteArray) {
        StringBuilder hexStringBuffer = new StringBuilder();
        for (byte b : byteArray) {
            hexStringBuffer.append(Integer.toHexString(Byte.toUnsignedInt(b)));
        }
        return hexStringBuffer.toString();
    }

    public List<HookCharacter> getAllCharacters(){
        Pointer listAddr = new Pointer(mapAddress + 10636);
        Memory listPtr = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, listAddr, listPtr, 4, null);

        Pointer listCountAddr = new Pointer(listPtr.getInt(0));
        Memory listSize = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, listCountAddr, listSize, 4, null);

        int size = listSize.getInt(0);

        Pointer listPtrAddr = new Pointer(listPtr.getInt(0) + 4);
        Memory listCharPtr = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, listPtrAddr, listCharPtr, 4, null);

        var firstPos = listCharPtr.getInt(0);

        var characters = new ArrayList<HookCharacter>();
        for(int i = 2; i < 40; i++){
            var location = readCharacterLocation(firstPos + (i * 4312));
            if(!location.equals(new Vector3f(0,0,0))){
                characters.add(new HookCharacter(location, readCharacterAngle(firstPos + (i * 4312)), 0));
            }
        }

        return characters;
    }
    public List<HookCharacter> getAllCharactersLIJ(){
        Pointer listCountAddr = new Pointer(0xaadee0);
        Memory listSize = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, listCountAddr, listSize, 4, null);

        int size = listSize.getInt(0);

        Pointer listPtrAddr = new Pointer(0xaadee4);
        Memory listCharPtr = new Memory(4);
        Kernel32.INSTANCE.ReadProcessMemory(process, listPtrAddr, listCharPtr, 4, null);

        var firstPos = listCharPtr.getInt(0);

        var characters = new ArrayList<HookCharacter>();
        for(int i = 2; i < 40; i++){
            var location = readCharacterLocation(firstPos + (i * 5552));
            if(!location.equals(new Vector3f(0,0,0))){
                characters.add(new HookCharacter(location, readCharacterAngle(firstPos + (i * 5552)), 0));
            }
        }

        return characters;
    }

    public Path getExecutablePath() {
        return Path.of(executablePath);
    }

    public Path getDirectory() {
        return Path.of(directory);
    }
}
