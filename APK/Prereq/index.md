# Developer Environment

1.  Install GIT.

    *  Add the GIT repository:
        
        ```
        $ sudo add-apt-repository ppa:git-core/ppa
        ```

    *  Run update: 

        ```
        $ sudo apt update
        ```
    
    *  Install GIT: 

        ```
        $ sudo apt install git
        ```
    * Set-up your identity:

        ```
        $ git config --global user.name "John Doe"
        $ git config --global user.email johndoe@example.com
        ```

2.  Android studio is a 32-bit program.  There is not a 64-bit release. If you are running a 64-bit version of Ubuntu, you must first install some 32-bit libraries with the following command:

    ```
    $ sudo apt-get install libc6:i386 libncurses5:i386 libstdc++6:i386 lib32z1 libbz2-1.0:i386
    ```

[Home](../../README.md)
