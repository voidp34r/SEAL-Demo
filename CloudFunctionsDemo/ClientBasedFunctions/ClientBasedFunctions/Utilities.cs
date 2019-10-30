// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

using Microsoft.Research.SEAL;
using System;
using System.IO;
using System.Text;
using System.Collections.Generic;

namespace SEALAzureFuncClient
{
    /// <summary>
    /// Utility methods.
    /// </summary>
    static class Utilities
    {
        /// <summary>
        /// Get a Matrix of integers from a MatrixData object
        /// </summary>
        /// <param name="md">MatrixData object to get the matrix from</param>
        /// <returns>Matrix of integers</returns>
        public static int[,] MatrixDataToMatrix(MatrixData md)
        {
            int rows = md.DataView.Table.Rows.Count;
            int cols = md.DataView.Table.Columns.Count;

            int[,] result = new int[rows, cols];
            for (int r = 0; r < rows; r++)
            {
                for (int c = 0; c < cols; c++)
                {
                    result[r, c] = (int)md.DataView.Table.Rows[r].ItemArray[c];
                }
            }

            return result;
        }

        /// <summary>
        /// Convert a Matrix to a Plaintext. Rows in the matrix are laid next to each
        /// other in a Plaintext.
        /// </summary>
        /// <param name="matrix">Matrix to convert to Plaintext</param>
        /// <param name="encoder">BatchEncoder used to encode the matrix data</param>
        /// <returns>Encrypted matrix</returns>
        public static Plaintext MatrixToPlaintext(int[,] matrix, BatchEncoder encoder)
        {
            int batchRowSize = (int)encoder.SlotCount / 2;
            int rows = matrix.GetLength(dimension: 0);
            int cols = matrix.GetLength(dimension: 1);
            int paddedColLength = FindNextPowerOfTwo((ulong)rows);
            int eltSeparation = batchRowSize / paddedColLength;
            long[] batchArray = new long[encoder.SlotCount];

            for (int r = 0; r < rows; r++)
            {
                for (int c = 0; c < cols; c++)
                {
                    batchArray[r * eltSeparation + c] = (long)matrix[r, c];
                }
            }

            Plaintext plain = new Plaintext();
            encoder.Encode(batchArray, plain);

            return plain;
        }

        /// <summary>
        /// Convert a Matrix to a twisted Plaintext. Rows in the matrix are laid next to
        /// each other in a Plaintext. The second batching row is duplicate of the first
        /// one but rotated by eltSeparation
        /// </summary>
        /// <param name="matrix">Matrix to convert to Plaintext</param>
        /// <param name="encoder">BatchEncoder used to encode the matrix data</param>
        /// <returns>Encrypted matrix</returns>
        public static Plaintext MatrixToTwistedPlaintext(int[,] matrix, BatchEncoder encoder)
        {
            int batchRowSize = (int)encoder.SlotCount / 2;
            int rows = matrix.GetLength(dimension: 0);
            int cols = matrix.GetLength(dimension: 1);
            int paddedColLength = FindNextPowerOfTwo((ulong)rows);
            int eltSeparation = batchRowSize / paddedColLength;
            long[] batchArray = new long[encoder.SlotCount];

            for (int r = 0; r < rows; r++)
            {
                for (int c = 0; c < cols; c++)
                {
                    batchArray[r * eltSeparation + c] = (long)matrix[r, c];
                    batchArray[batchRowSize + ((batchRowSize + (r - 1) * eltSeparation) % batchRowSize) + c] = (long)matrix[r, c];
                }
            }

            Plaintext plain = new Plaintext();
            encoder.Encode(batchArray, plain);

            return plain;
        }

        /// <summary>
        /// Convert a Plaintext to a matrix with the given rows and columns
        /// </summary>
        /// <param name="plain">Plaintext to convert</param>
        /// <param name="rows">Rows in the matrix</param>
        /// <param name="cols">Columns in the matrix</param>
        /// <param name="encoder">BatchEncoder used to encode the matrix data</param>
        public static int[,] PlaintextToMatrix(Plaintext plain, int rows, int cols, BatchEncoder encoder)
        {
            int batchRowSize = (int)encoder.SlotCount / 2;
            int paddedRowLength = FindNextPowerOfTwo((ulong)rows);
            int eltSeparation = batchRowSize / paddedRowLength;

            List<long> batchArray = new List<long>();
            encoder.Decode(plain, batchArray);

            int[,] matresult = new int[rows, cols];
            for (int r = 0; r < rows; r++)
            {
                for (int c = 0; c < cols; c++)
                {
                    matresult[r, c] = (int)batchArray[r * eltSeparation + c];
                }
            }

            return matresult;
        }

        /// <summary>
        /// Set the indicated Matrix row as the coefficients of a Plaintext.
        /// </summary>
        /// <param name="matrix">Matrix to get a row from</param>
        /// <param name="row">Row to encrypt</param>
        /// <param name="repl">How many times to replicate the row values</param>
        /// <param name="encoder">BatchEncoder used to encode the matrix data</param>
        /// <returns>Encrypted matrix row</returns>
        public static Plaintext RowToPlaintext(int[,] matrix, int row, int repl, BatchEncoder encoder)
        {
            int batchRowSize = (int)encoder.SlotCount / 2;
            int cols = matrix.GetLength(dimension: 1);
            int paddedLength = FindNextPowerOfTwo((ulong)cols);
            int eltSeparation = batchRowSize / paddedLength;

            if (repl < 0 || repl > eltSeparation)
            {
                throw new ArgumentException("repl out of bounds");
            }

            long[] rowToEncrypt = new long[batchRowSize];
            for (int c = 0; c < cols; c++)
            {
                for (int j = 0; j < repl; j++)
                {
                    rowToEncrypt[eltSeparation * c + j] = matrix[row, c];
                }
            }

            Plaintext plain = new Plaintext();
            encoder.Encode(rowToEncrypt, plain);

            return plain;
        }

        /// <summary>
        /// Set the indicated Matrix rows as the coefficients of Plaintexts. Two rows will
        /// always be encoded in a single Plaintext into the two batching "rows".
        /// </summary>
        /// <param name="matrix">Matrix to get a row from</param>
        /// <param name="repl">How many times to replicate the row values</param>
        /// <param name="encoder">BatchEncoder used to encode the matrix data</param>
        /// <returns>Encrypted matrix rows</returns>
        public static List<Plaintext> RowsToPlaintexts(int[,] matrix, int repl, BatchEncoder encoder)
        {
            int batchRowCount = 2;
            int batchRowSize = (int)encoder.SlotCount / 2;
            int cols = matrix.GetLength(dimension: 1);
            int rows = matrix.GetLength(dimension: 0);
            int paddedLength = FindNextPowerOfTwo((ulong)cols);
            int eltSeparation = batchRowSize / paddedLength;

            if (repl < 0 || repl > eltSeparation)
            {
                throw new ArgumentException("repl out of bounds");
            }

            List<Plaintext> result = new List<Plaintext>();
            int r = 0;
            while (r < rows)
            {
                long[] batchArray = new long[encoder.SlotCount];
                for (int batchRowIndex = 0; batchRowIndex < batchRowCount && r < rows; batchRowIndex++, r++)
                {
                    for (int c = 0; c < cols; c++)
                    {
                        for (int j = 0; j < repl; j++)
                        {
                            batchArray[batchRowIndex * batchRowSize + eltSeparation * c + j] = matrix[r, c];
                        }
                    }
                }

                Plaintext plain = new Plaintext();
                encoder.Encode(batchArray, plain);
                result.Add(plain);
            }

            return result;
        }

        /// <summary>
        /// Set the indicated Matrix column as the coefficients of a Plaintext
        /// (in inverse order).
        /// </summary>
        /// <param name="matrix">Matrix to get a column from</param>
        /// <param name="col">Column to encrypt</param>
        /// <param name="encoder">BatchEncoder used to encode the matrix data</param>
        /// <returns>Encrypted inverted matrix column</returns>
        public static Plaintext InvertedColumnToPlaintext(int[,] matrix, int col, BatchEncoder encoder)
        {
            int batchRowSize = (int)encoder.SlotCount / 2;
            int rows = matrix.GetLength(dimension: 0);
            int paddedLength = FindNextPowerOfTwo((ulong)rows);
            int eltSeparation = batchRowSize / paddedLength;
            long[] colToEncrypt = new long[batchRowSize];
            for (int r = 0; r < rows; r++)
            {
                colToEncrypt[eltSeparation * r] = matrix[r, col];
            }

            Plaintext plain = new Plaintext();
            encoder.Encode(colToEncrypt, plain);

            return plain;
        }

        /// <summary>
        /// Get a string representing a Matrix
        /// </summary>
        /// <param name="matrix">Matrix to convert</param>
        /// <returns>String representation of the given Matrix</returns>
        public static string MatrixToString(int[,] matrix)
        {
            int rows = matrix.GetLength(dimension: 0);
            int cols = matrix.GetLength(dimension: 1);

            StringBuilder matresult = new StringBuilder();
            for (int r = 0; r < rows; r++)
            {
                for (int c = 0; c < cols; c++)
                {
                    matresult.Append(matrix[r, c]);
                    if (c < (cols - 1))
                        matresult.Append(", ");
                }

                matresult.Append("\n");
            }

            return matresult.ToString();
        }

        /// <summary>
        /// Convert a Plaintext to a Base64-encoded Ciphertext string.
        /// </summary>
        /// <param name="plain">Plaintext to encrypt, save, and convert to Base64</param>
        /// <returns>Base64 string representing the Ciphertext</returns>
        public static string EncryptToBase64(Plaintext plain, Encryptor encryptor)
        {
            using (MemoryStream ms = new MemoryStream())
            {
                encryptor.EncryptSymmetricSave(plain, ms);
                byte[] bytes = ms.ToArray();
                return Convert.ToBase64String(bytes);
            }
        }

        /// <summary>
        /// Convert an enumeration of Plaintexts to a Base64-encoded Ciphertext string.
        /// </summary>
        /// <param name="plains">Plaintexts to encrypt, save, and convert to Base64</param>
        /// <returns>Base64 string representing the Ciphertexts</returns>
        public static string EncryptToBase64(
            IEnumerable<Plaintext> plains, Encryptor encryptor)
        {
            using (MemoryStream ms = new MemoryStream())
            {
                foreach (Plaintext plain in plains)
                {
                    encryptor.EncryptSymmetricSave(plain, ms);
                }
                byte[] bytes = ms.ToArray();
                return Convert.ToBase64String(bytes);
            }
        }

        /// <summary>
        /// Convert a Base64 string to a Ciphertext
        /// </summary>
        /// <param name="b64">Base 64 string</param>
        /// <param name="context">SEALContext to verify resulting Ciphertext is valid for the SEALContext</param>
        /// <returns>Decoded Ciphertext</returns>
        public static Ciphertext Base64ToCiphertext(string b64, SEALContext context)
        {
            Ciphertext result = new Ciphertext();
            byte[] bytes = Convert.FromBase64String(b64);
            using (MemoryStream ms = new MemoryStream(bytes))
            {
                result.Load(context, ms);
            }

            return result;
        }

        /// <summary>
        /// Convert a Base64 string to a vector of Ciphertexts
        /// </summary>
        /// <param name="b64">Base 64 string</param>
        /// <param name="context">SEALContext to verify resulting Ciphertexts are valid for the SEALContext</param>
        /// <returns>Decoded Ciphertext</returns>
        public static List<Ciphertext> Base64ToCiphertextList(string b64, SEALContext context)
        {
            List<Ciphertext> result = new List<Ciphertext>();
            byte[] bytes = Convert.FromBase64String(b64);
            using (MemoryStream ms = new MemoryStream(bytes))
            {
                while (true)
                {
                    Ciphertext currCipher = new Ciphertext();
                    try
                    {
                        currCipher.Load(context, ms);
                    }
                    catch (Exception)
                    {
                        break;
                    }
                    result.Add(currCipher);
                }
            }

            return result;
        }

        /// <summary>
        /// Convert a RelinKeys object to a Base64 string
        /// </summary>
        /// <param name="rlk">RelinKeys to convert</param>
        /// <returns>Base64 string representing the RelinKeys</returns>
        public static string RelinKeysToBase64(KeyGenerator keygen)
        {
            using (MemoryStream ms = new MemoryStream())
            {
                // Saving directly to stream; this compresses the size in half
                keygen.RelinKeysSave(ms);
                byte[] bytes = ms.ToArray();
                return Convert.ToBase64String(bytes);
            }
        }

        /// <summary>
        /// Convert a GaloisKeys object to a Base64 string
        /// </summary>
        /// <param name="galk">GaloisKeys to convert</param>
        /// <returns>Base64 string representing the GaloisKeys</returns>
        public static string GaloisKeysToBase64(KeyGenerator keygen, List<int> rotations)
        {
            using (MemoryStream ms = new MemoryStream())
            {
                // Saving directly to stream; this compresses the size in half
                keygen.GaloisKeysSave(rotations, ms);
                byte[] bytes = ms.ToArray();
                return Convert.ToBase64String(bytes);
            }
        }

        /// <summary>
        /// Find the next power of two that is equal or larger to the provided value
        /// </summary>
        /// <param name="value">Value to find the next power of two for</param>
        public static int FindNextPowerOfTwo(ulong value)
        {
            value--;
            value |= value >> 1;
            value |= value >> 2;
            value |= value >> 4;
            value |= value >> 8;
            value |= value >> 16;
            value |= value >> 32;
            value++;

            return (int)value;
        }

        /// <summary>
        /// Get the Transpose of the given matrix
        /// </summary>
        /// <param name="matrix">Input matrix</param>
        /// <returns>Transpose of the input matrix</returns>
        public static int[,] Transpose(int[,] matrix)
        {
            int inRows = matrix.GetLength(dimension: 0);
            int inCols = matrix.GetLength(dimension: 1);
            int[,] result = new int[inCols, inRows];
            
            for (int r = 0; r < inRows; r++)
            {
                for (int c = 0; c < inCols; c++)
                {
                    result[c, r] = matrix[r, c];
                }
            }

            return result;
        }
    }
}
