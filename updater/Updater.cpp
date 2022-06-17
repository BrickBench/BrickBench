#pragma comment(lib, "user32.lib")

#include <iostream>
#include <string>
#include <filesystem>
#include <chrono>
#include <thread>
#include <windows.h>

namespace fs = std::filesystem;

int main(int argc, char** argv)
{
    if (argc != 3) {
        int msgboxID = MessageBoxA(
            NULL,
            "This updater is not meant to be run standalone. To update\nBrickBench, use the updater in the main program.",
            "Updater warning",
            MB_ICONWARNING | MB_OK | MB_DEFBUTTON1
        );

        return 1;
    }

    fs::path newInstall(argv[2]);
    fs::path absNewInstall = fs::absolute(newInstall);

    fs::path oldInstall(argv[1]);
    fs::path absOldInstall = fs::absolute(oldInstall);

	std::cout << "Updating from " << absNewInstall << std::endl;
    if (!fs::exists(absNewInstall)) {
        return 0;
    }

    std::this_thread::sleep_for(std::chrono::seconds(3));

    std::error_code error;
    fs::copy(absNewInstall, absOldInstall, fs::copy_options::overwrite_existing | fs::copy_options::recursive, error);
    std::cout << error.message() << std::endl;
}
