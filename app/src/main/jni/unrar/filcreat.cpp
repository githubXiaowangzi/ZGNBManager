#include "rar.hpp"

bool FileCreate(RAROptions *Cmd, File *NewFile, char *Name, wchar *NameW,
                OVERWRITE_MODE Mode, bool *UserReject, int64 FileSize,
                uint FileTime) {
    if (UserReject != NULL)
        *UserReject = false;
#if defined(_WIN_32) && !defined(_WIN_CE)
    bool ShortNameChanged = false;
#endif
    while (FileExist(Name, NameW)) {
#if defined(_WIN_32) && !defined(_WIN_CE)
        if (!ShortNameChanged) {
            ShortNameChanged = true;
            if (UpdateExistingShortName(Name, NameW))
                continue;
        }
#endif
        if (Mode == OVERWRITE_NONE) {
            if (UserReject != NULL)
                *UserReject = true;
            return(false);
        }
#ifdef SILENT
        Mode = OVERWRITE_ALL;
#endif
        if (Cmd->AllYes || Mode == OVERWRITE_ALL)
            break;
        if (Mode == OVERWRITE_DEFAULT || Mode == OVERWRITE_FORCE_ASK) {
            eprintf(St(MFileExists), Name);
            int Choice = Ask(St(MYesNoAllRenQ));
            if (Choice == 1)
                break;
            if (Choice == 2) {
                if (UserReject != NULL)
                    *UserReject = true;
                return(false);
            }
            if (Choice == 3) {
                Cmd->Overwrite = OVERWRITE_ALL;
                break;
            }
            if (Choice == 4) {
                if (UserReject != NULL)
                    *UserReject = true;
                Cmd->Overwrite = OVERWRITE_NONE;
                return(false);
            }
            if (Choice == 5) {
                mprintf(St(MAskNewName));

                char NewName[NM];
#ifdef  _WIN_32
                File SrcFile;
                SrcFile.SetHandleType(FILE_HANDLESTD);
                int Size = SrcFile.Read(NewName, sizeof(NewName) - 1);
                NewName[Size] = 0;
                OemToChar(NewName, NewName);
#else
                if (fgets(NewName, sizeof(NewName), stdin) == NULL) {
                    // Process fgets failure as if user answered 'No'.
                    if (UserReject != NULL)
                        *UserReject = true;
                    return(false);
                }
#endif
                RemoveLF(NewName);
                if (PointToName(NewName) == NewName)
                    strcpy(PointToName(Name), NewName);
                else
                    strcpy(Name, NewName);
                if (NameW != NULL)
                    *NameW = 0;
                continue;
            }
            if (Choice == 6)
                ErrHandler.Exit(USER_BREAK);
        }
        if (Mode == OVERWRITE_AUTORENAME) {
            if (GetAutoRenamedName(Name)) {
                if (NameW != NULL)
                    *NameW = 0;
            } else
                Mode = OVERWRITE_DEFAULT;
            continue;
        }
    }
    if (NewFile != NULL && NewFile->Create(Name, NameW))
        return(true);
    PrepareToDelete(Name, NameW);
    CreatePath(Name, NameW, true);
    return(NewFile != NULL ? NewFile->Create(Name, NameW) : DelFile(Name, NameW));
}


bool GetAutoRenamedName(char *Name) {
    char NewName[NM];

    if (strlen(Name) > sizeof(NewName) - 10)
        return(false);
    char *Ext = GetExt(Name);
    if (Ext == NULL)
        Ext = Name + strlen(Name);
    for (int FileVer = 1;; FileVer++) {
        sprintf(NewName, "%.*s(%d)%s", int(Ext - Name), Name, FileVer, Ext);
        if (!FileExist(NewName)) {
            strcpy(Name, NewName);
            break;
        }
        if (FileVer >= 1000000)
            return(false);
    }
    return(true);
}


#if defined(_WIN_32) && !defined(_WIN_CE)
bool UpdateExistingShortName(char *Name, wchar *NameW) {
    FindData fd;
    if (!FindFile::FastFind(Name, NameW, &fd))
        return(false);
    if (*fd.Name == 0 || *fd.ShortName == 0)
        return(false);
    if (stricomp(PointToName(fd.Name), fd.ShortName) == 0 ||
            stricomp(PointToName(Name), fd.ShortName) != 0)
        return(false);

    char NewName[NM];
    for (int I = 0; I < 10000; I += 123) {
        strncpyz(NewName, Name, ASIZE(NewName));
        sprintf(PointToName(NewName), "rtmp%d", I);
        if (!FileExist(NewName))
            break;
    }
    if (FileExist(NewName))
        return(false);
    char FullName[NM];
    strncpyz(FullName, Name, ASIZE(FullName));
    strcpy(PointToName(FullName), PointToName(fd.Name));
    if (!MoveFile(FullName, NewName))
        return(false);
    File KeepShortFile;
    bool Created = false;
    if (!FileExist(Name))
        Created = KeepShortFile.Create(Name);
    MoveFile(NewName, FullName);
    if (Created) {
        KeepShortFile.Close();
        KeepShortFile.Delete();
    }
    return(true);
}
#endif
